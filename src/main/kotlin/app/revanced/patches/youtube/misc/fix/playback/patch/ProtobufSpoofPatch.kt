package app.revanced.patches.youtube.misc.fix.playback.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.fix.playback.fingerprints.ProtobufParameterBuilderFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH

@Patch
@Name("protobuf-spoof")
@Description("Spoofs the protobuf to prevent playback issues.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ProtobufSpoofPatch : BytecodePatch(
    listOf(ProtobufParameterBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

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
                        invoke-static {p$protobufParam}, $MISC_PATH/ProtobufSpoofPatch;->getProtobufOverride(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p$protobufParam
                    """
                )
            }
        } ?: return ProtobufParameterBuilderFingerprint.toErrorResult()

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
