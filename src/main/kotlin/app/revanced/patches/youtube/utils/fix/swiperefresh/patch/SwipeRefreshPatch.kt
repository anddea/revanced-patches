package app.revanced.patches.youtube.utils.fix.swiperefresh.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.youtube.utils.fix.swiperefresh.fingerprint.SwipeRefreshLayoutFingerprint
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

class SwipeRefreshPatch : BytecodePatch(
    listOf(SwipeRefreshLayoutFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        SwipeRefreshLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$register, 0x0"
                )
            }
        } ?: throw SwipeRefreshLayoutFingerprint.exception

    }
}
