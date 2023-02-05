package app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.bytecode.fingerprints.FullscreenViewAdderFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.fingerprints.LayoutConstructorFingerprint
import app.revanced.shared.util.integrations.Constants.FULLSCREEN_LAYOUT
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Name("hide-fullscreen-panels-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideFullscreenPanelsBytecodePatch : BytecodePatch(
    listOf(
        FullscreenViewAdderFingerprint,
        LayoutConstructorFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        FullscreenViewAdderFingerprint.result?.let {
            with (it.mutableMethod) {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val register = (implementation!!.instructions[endIndex] as Instruction35c).registerD

                for (i in 1..3) removeInstruction(endIndex - i)

                addInstructions(
                    endIndex - 3, """
                        invoke-static {}, $FULLSCREEN_LAYOUT->hideFullscreenPanels()I
                        move-result v$register
                    """
                )
            }
        } ?: return FullscreenViewAdderFingerprint.toErrorResult()

        LayoutConstructorFingerprint.result?.mutableMethod?.let { method ->
            val invokeIndex = method.implementation!!.instructions.indexOfFirst {
                it.opcode.ordinal == Opcode.INVOKE_VIRTUAL.ordinal &&
                        ((it as? BuilderInstruction35c)?.reference.toString() ==
                                "Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V")
            }

            method.addInstructions(
                invokeIndex, """
                    invoke-static {}, $FULLSCREEN_LAYOUT->hideFullscreenPanel()Z
                    move-result v15
                    if-nez v15, :hidden
                """, listOf(ExternalLabel("hidden", method.instruction(invokeIndex + 1)))
            )
        } ?: return LayoutConstructorFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
