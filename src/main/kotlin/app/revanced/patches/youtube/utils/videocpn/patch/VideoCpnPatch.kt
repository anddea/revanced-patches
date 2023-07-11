package app.revanced.patches.youtube.utils.videocpn.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.videocpn.fingerprint.OrganicPlaybackContextModelFingerprint

class VideoCpnPatch : BytecodePatch(
    listOf(OrganicPlaybackContextModelFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        insertMethod = OrganicPlaybackContextModelFingerprint.result?.mutableMethod
            ?: return OrganicPlaybackContextModelFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    companion object {
        private lateinit var insertMethod: MutableMethod

        fun injectCall(
            methodDescriptor: String
        ) {
            insertMethod.addInstructions(
                2,
                "invoke-static {p1,p2}, $methodDescriptor"
            )
        }
    }
}

