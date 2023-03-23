package app.revanced.patches.shared.patch.videoads

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.fingerprints.LegacyVideoAdsFingerprint
import app.revanced.patches.shared.fingerprints.MainstreamVideoAdsFingerprint

@Name("general-video-ads-patch")
@Version("0.0.1")
class GeneralVideoAdsPatch : BytecodePatch(
    listOf(
        LegacyVideoAdsFingerprint,
        MainstreamVideoAdsFingerprint,
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val legacyVideoAdsResult = LegacyVideoAdsFingerprint.result ?: return LegacyVideoAdsFingerprint.toErrorResult()
        val mainstreamVideoAdsResult = MainstreamVideoAdsFingerprint.result ?: return MainstreamVideoAdsFingerprint.toErrorResult()

        legacyVideoAdsMethod =
            context.toMethodWalker(legacyVideoAdsResult.method)
                .nextMethod(13, true)
                .getMethod() as MutableMethod

        mainstreamVideoAdsMethod = mainstreamVideoAdsResult.mutableMethod

        return PatchResultSuccess()
    }

    internal companion object {
        lateinit var legacyVideoAdsMethod: MutableMethod
        lateinit var mainstreamVideoAdsMethod: MutableMethod

        fun injectLegacyAds(
            descriptor: String
        ) {
            legacyVideoAdsMethod.addInstructions(
                0, """
                    invoke-static {}, $descriptor
                    move-result v1
                    """
            )
        }

        fun injectMainstreamAds(
            descriptor: String
        ) {
            mainstreamVideoAdsMethod.addInstructions(
                0, """
                    invoke-static {}, $descriptor
                    move-result v0
                    if-nez v0, :show_video_ads
                    return-void
                    """, listOf(ExternalLabel("show_video_ads", mainstreamVideoAdsMethod.instruction(0)))
            )
        }

    }
}