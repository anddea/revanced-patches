package app.revanced.patches.youtube.layout.player.endscreencards.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.layout.player.endscreencards.bytecode.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.injectHideCall
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

        fun MethodFingerprint.injectHideCalls() {
            val layoutResult = result!!
            val layoutMethod = layoutResult.mutableMethod

            val checkCastIndex = layoutResult.scanResult.patternScanResult!!.endIndex
            val viewRegister = (layoutMethod.instruction(checkCastIndex) as Instruction21c).registerA

            layoutMethod.implementation!!.injectHideCall(checkCastIndex + 1, viewRegister, "layout/PlayerLayoutPatch", "hideEndscreen")
        }
        
        listOf(LayoutCircleFingerprint, LayoutIconFingerprint, LayoutVideoFingerprint).forEach(MethodFingerprint::injectHideCalls)

        return PatchResultSuccess()
    }
}
