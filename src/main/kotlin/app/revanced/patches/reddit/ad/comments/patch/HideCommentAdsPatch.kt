package app.revanced.patches.reddit.ad.comments.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.ad.comments.fingerprints.HideCommentAdsFingerprint

class HideCommentAdsPatch : BytecodePatch(
    listOf(HideCommentAdsFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        HideCommentAdsFingerprint.result?.let {
            with(
                context
                    .toMethodWalker(it.method)
                    .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                    .getMethod() as MutableMethod
            ) {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $INTEGRATION_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :show
                        new-instance v0, Ljava/lang/Object;
                        invoke-direct {v0}, Ljava/lang/Object;-><init>()V
                        return-object v0
                        """, ExternalLabel("show", getInstruction(0))
                )
            }
        } ?: throw HideCommentAdsFingerprint.exception

    }

    private companion object {
        private const val INTEGRATION_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/GeneralAdsPatch;" +
                    "->hideCommentAds()Z"
    }
}
