package app.revanced.patches.youtube.misc.protobufpoof.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.misc.protobufpoof.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.misc.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.util.integrations.Constants.MISC_PATH

@Patch
@Name("protobuf-spoof")
@Description("Spoofs the protobuf to prevent playback issues.")
@DependsOn(
    [
        LegacyVideoIdPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ProtobufSpoofPatch : BytecodePatch(
    listOf(
        BadResponseFingerprint,
        ProtobufParameterBuilderFingerprint,
        SubtitleWindowFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        // hook parameter
        ProtobufParameterBuilderFingerprint.result?.let {
            with (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                .getMethod() as MutableMethod
            ) {
                val protobufParam = 3

                addInstructions(
                    0,
                    """
                        invoke-static {p$protobufParam}, $MISC_PATH/ProtobufSpoofPatch;->overrideProtobufParameter(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p$protobufParam
                    """
                )
            }
        } ?: return ProtobufParameterBuilderFingerprint.toErrorResult()

        // hook video playback result
        BadResponseFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "invoke-static {}, $MISC_PATH/ProtobufSpoofPatch;->switchProtobufSpoof()V"
        ) ?: return BadResponseFingerprint.toErrorResult()

        // fix protobuf spoof side issue
        SubtitleWindowFingerprint.result?.mutableMethod?.addInstructions(
            0,
            """
                invoke-static {p1, p2, p3, p4, p5}, $MISC_PATH/ProtobufSpoofPatch;->getSubtitleWindowSettingsOverride(IIIZZ)[I
                move-result-object v0
                const/4 v1, 0x0
                aget p1, v0, v1     # ap, anchor position
                const/4 v1, 0x1
                aget p2, v0, v1     # ah, horizontal anchor
                const/4 v1, 0x2
                aget p3, v0, v1     # av, vertical anchor
            """
        ) ?: return SubtitleWindowFingerprint.toErrorResult()

        // Hook video id, required for subtitle fix.
        LegacyVideoIdPatch.injectCall("$MISC_PATH/ProtobufSpoofPatch;->setCurrentVideoId(Ljava/lang/String;)V")

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_PROTOBUF_SPOOF"
            )
        )
        SettingsPatch.updatePatchStatus("protobuf-spoof")

        return PatchResultSuccess()
    }
}
