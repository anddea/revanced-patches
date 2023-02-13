package app.revanced.patches.youtube.layout.player.endscreencards.bytecode.patch

import app.revanced.extensions.injectHideCall
import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.player.endscreencards.bytecode.fingerprints.*
import org.jf.dexlib2.iface.instruction.formats.Instruction21c

@Name("hide-endscreen-cards-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideEndscreenCardsBytecodePatch : BytecodePatch(
    listOf(
        LayoutCircleFingerprint,
        LayoutIconFingerprint,
        LayoutVideoFingerprint,
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        fun MethodFingerprintResult.injectHideCalls() {
            val index = this.scanResult.patternScanResult!!.endIndex
            with (this.mutableMethod) {
                val register = (this.instruction(index) as Instruction21c).registerA
                this.implementation!!.injectHideCall(index + 1, register, "layout/PlayerLayoutPatch", "hideEndscreen")
            }
        }
        
        listOf(
            LayoutCircleFingerprint,
            LayoutIconFingerprint,
            LayoutVideoFingerprint
        ).forEach {
            it.result?.injectHideCalls() ?: return it.toErrorResult()
        }

        return PatchResultSuccess()
    }
}
