package app.revanced.patches.reddit.ad.comments.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.reddit.ad.comments.fingerprints.HideCommentAdsFingerprint

class HideCommentAdsPatch : BytecodePatch(
    listOf(HideCommentAdsFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        HideCommentAdsFingerprint.result?.let {
            with(
                context
                    .toMethodWalker(it.method)
                    .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                    .getMethod() as MutableMethod
            ) {

                addInstructions(
                    0, """
                        new-instance v0, Ljava/lang/Object;
                        invoke-direct {v0}, Ljava/lang/Object;-><init>()V
                        return-object v0
                        """
                )
            }
        } ?: return HideCommentAdsFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
