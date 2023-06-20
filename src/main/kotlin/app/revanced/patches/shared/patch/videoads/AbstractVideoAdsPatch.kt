package app.revanced.patches.shared.patch.videoads

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.fingerprints.videoads.LegacyVideoAdsFingerprint
import app.revanced.patches.shared.fingerprints.videoads.MainstreamVideoAdsFingerprint

@Name("abstract-video-ads-patch")
@Version("0.0.1")
abstract class AbstractVideoAdsPatch(
    private val descriptor: String
) : BytecodePatch(
    listOf(
        LegacyVideoAdsFingerprint,
        MainstreamVideoAdsFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        LegacyVideoAdsFingerprint.result?.let {
            (context.toMethodWalker(it.method)
                .nextMethod(13, true)
                .getMethod() as MutableMethod).apply {
                addInstructions(
                    0, """
                            invoke-static {}, $descriptor
                            move-result v1
                            """
                )
            }
        } ?: return LegacyVideoAdsFingerprint.toErrorResult()

        MainstreamVideoAdsFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $descriptor
                        move-result v0
                        if-nez v0, :show_video_ads
                        return-void
                        """, ExternalLabel("show_video_ads", getInstruction(0))
                )
            }
        }

        return PatchResultSuccess()
    }
}