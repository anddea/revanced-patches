package app.revanced.shared.patches.videoads

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.shared.fingerprints.LegacyVideoAdsFingerprint
import app.revanced.shared.fingerprints.MainstreamVideoAdsFingerprint
import app.revanced.shared.fingerprints.MainstreamVideoAdsParentFingerprint
import app.revanced.shared.extensions.toErrorResult

@Name("general-video-ads-patch")
@Version("0.0.1")
class GeneralVideoAdsPatch : BytecodePatch(
    listOf(
        LegacyVideoAdsFingerprint,
        MainstreamVideoAdsParentFingerprint,
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val LegacyVideoAdsResult = LegacyVideoAdsFingerprint.result ?: return LegacyVideoAdsFingerprint.toErrorResult()

        LegacyVideoAdsMethod =
            context.toMethodWalker(LegacyVideoAdsResult.method)
                .nextMethod(13, true)
                .getMethod() as MutableMethod

        MainstreamVideoAdsFingerprint.resolve(context, MainstreamVideoAdsParentFingerprint.result!!.classDef)

        val MainstreamAdsResult = MainstreamVideoAdsFingerprint.result ?: return MainstreamVideoAdsFingerprint.toErrorResult()

        MainstreamVideoAdsMethod = MainstreamAdsResult.mutableMethod

        InsertIndex = MainstreamAdsResult.scanResult.patternScanResult!!.endIndex

        return PatchResultSuccess()
    }

    internal companion object {
        var InsertIndex: Int = 0

        lateinit var LegacyVideoAdsMethod: MutableMethod
        lateinit var MainstreamVideoAdsMethod: MutableMethod

        fun injectLegacyAds(
            descriptor: String
        ) {
            LegacyVideoAdsMethod.addInstructions(
                0, """
                    invoke-static {}, $descriptor
                    move-result v1
            """
            )
        }

        fun injectMainstreamAds(
            descriptor: String
        ) {
            MainstreamVideoAdsMethod.addInstructions(
                InsertIndex, """
                    invoke-static {}, $descriptor
                    move-result v1
                    if-nez v1, :show_video_ads
                    return-void
            """, listOf(ExternalLabel("show_video_ads", MainstreamVideoAdsMethod.instruction(InsertIndex)))
            )
        }

    }
}