package app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.layout.fullscreen.fullscreenbuttoncontainer.bytecode.patch.HideFullscreenButtonContainerBytecodePatch
import app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.fingerprints.FullscreenViewAdderFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("hide-fullscreen-panels")
@Description("Hides video description and comments panel in fullscreen view.")
@DependsOn(
    [
        HideFullscreenButtonContainerBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideFullscreenPanelsPatch : BytecodePatch(
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
                        invoke-static {}, $FULLSCREEN->hideFullscreenPanels()I
                        move-result v$register
                    """
                )
            }
        } ?: return FullscreenViewAdderFingerprint.toErrorResult()

        LayoutConstructorFingerprint.result?.mutableMethod?.let { method ->
            val instructions = method.implementation!!.instructions
            val registerIndex = instructions.indexOfFirst {
                it.opcode == Opcode.CONST_STRING &&
                        (it as BuilderInstruction21c).reference.toString() == "1.0x"
            }
            val dummyRegister = (instructions[registerIndex] as Instruction21c).registerA

            val invokeIndex = method.implementation!!.instructions.indexOfFirst {
                it.opcode.ordinal == Opcode.INVOKE_VIRTUAL.ordinal &&
                        ((it as? BuilderInstruction35c)?.reference.toString() ==
                                "Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V")
            }

            method.addInstructions(
                invokeIndex, """
                    invoke-static {}, $FULLSCREEN->showFullscreenTitle()Z
                    move-result v$dummyRegister
                    if-eqz v$dummyRegister, :hidden
                """, listOf(ExternalLabel("hidden", method.instruction(invokeIndex + 1)))
            )
        } ?: return LayoutConstructorFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_FULLSCREEN_PANELS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-fullscreen-panels")

        return PatchResultSuccess()
    }
}
