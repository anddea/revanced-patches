package app.revanced.patches.youtube.utils.fix.parameter.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.BadResponseFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.ProtobufParameterBuilderFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.SubtitleWindowFingerprint
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.videoid.general.patch.VideoIdPatch
import app.revanced.util.integrations.Constants.MISC_PATH

@Name("spoof-player-parameters")
@Description("Spoofs player parameters to prevent playback issues.")
@DependsOn(
    [
        PlayerTypeHookPatch::class,
        VideoIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SpoofPlayerParameterPatch : BytecodePatch(
    listOf(
        BadResponseFingerprint,
        ProtobufParameterBuilderFingerprint,
        SubtitleWindowFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        // hook parameter
        ProtobufParameterBuilderFingerprint.result?.let {
            (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                .getMethod() as MutableMethod
                    ).apply {
                    val protobufParam = 3

                    addInstructions(
                        0,
                        """
                        invoke-static {p$protobufParam}, $INTEGRATIONS_CLASS_DESCRIPTOR->overridePlayerParameter(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p$protobufParam
                    """
                    )
                }
        } ?: return ProtobufParameterBuilderFingerprint.toErrorResult()

        // hook video playback result
        BadResponseFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->switchPlayerParameters()V"
        ) ?: return BadResponseFingerprint.toErrorResult()

        // fix protobuf spoof side issue
        SubtitleWindowFingerprint.result?.mutableMethod?.addInstructions(
            0,
            """
                invoke-static {p1, p2, p3, p4, p5}, $INTEGRATIONS_CLASS_DESCRIPTOR->getSubtitleWindowSettingsOverride(IIIZZ)[I
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
        VideoIdPatch.injectCall("$MISC_PATH/SpoofPlayerParameterPatch;->setCurrentVideoId(Ljava/lang/String;)V")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: SPOOF_PLAYER_PARAMETER"
            )
        )

        return PatchResultSuccess()
    }
    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$MISC_PATH/SpoofPlayerParameterPatch;"
    }
}
