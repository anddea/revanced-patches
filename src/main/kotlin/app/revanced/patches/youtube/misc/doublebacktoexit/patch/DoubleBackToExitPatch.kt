package app.revanced.patches.youtube.misc.doublebacktoexit.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.doublebacktoexit.fingerprint.RecyclerViewFingerprint
import app.revanced.patches.youtube.misc.doublebacktoexit.fingerprint.ScrollPositionFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.patches.gestures.PredictiveBackGesturePatch
import app.revanced.shared.util.bytecode.BytecodeHelper
import app.revanced.shared.util.integrations.Constants.UTILS_PATH

import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.Opcode

@Name("enable-double-back-to-exit")
@Description("Enable double back to exit.")
@YouTubeCompatibility
@Version("0.0.1")
@DependsOn([PredictiveBackGesturePatch::class])
class DoubleBackToExitPatch : BytecodePatch(
    listOf(
        RecyclerViewFingerprint,
        ScrollPositionFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val DTE_CLASS_DESCRIPTOR = "$UTILS_PATH/DoubleBackToExitPatch;"

        val scrollPositionResult = ScrollPositionFingerprint.result!!
        val scrollPositionMethod =
            context.toMethodWalker(scrollPositionResult.method)
                .nextMethod(scrollPositionResult.scanResult.patternScanResult!!.startIndex + 1, true)
                .getMethod() as MutableMethod
        val backToExitInstructions = scrollPositionMethod.implementation!!.instructions

        for ((index, instruction) in backToExitInstructions.withIndex()) {
            if (instruction.opcode != Opcode.CONST_4) continue
            
            scrollPositionMethod.addInstruction(
                index + 2,
                "invoke-static {}, $DTE_CLASS_DESCRIPTOR->onCreate()V"
            )

            break
        }

        val recyclerViewResult = RecyclerViewFingerprint.result ?: return RecyclerViewFingerprint.toErrorResult()
        val recyclerViewMethod = recyclerViewResult.mutableMethod
        val recyclerViewEndIndex = recyclerViewResult.scanResult.patternScanResult!!.endIndex

        recyclerViewMethod.addInstruction(
            recyclerViewEndIndex,
            "invoke-static {}, $DTE_CLASS_DESCRIPTOR->onDestroy()V"
        )

        BytecodeHelper.injectBackPressed(context)

        return PatchResultSuccess()
    }
}
