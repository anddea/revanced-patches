package app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.fingerprints.FullscreenViewAdderFingerprint
import app.revanced.patches.youtube.layout.fullscreen.quickactions.patch.QuickActionsPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import app.revanced.util.integrations.Constants.FULLSCREEN
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("hide-fullscreen-panels")
@Description("Hides video description and comments panel in fullscreen view.")
@DependsOn(
    [
        QuickActionsPatch::class,
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
            it.mutableMethod.apply {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<Instruction35c>(endIndex).registerD

                for (i in 1..3) removeInstruction(endIndex - i)

                addInstructions(
                    endIndex - 3, """
                        invoke-static {}, $FULLSCREEN->hideFullscreenPanels()I
                        move-result v$register
                    """
                )
            }
        } ?: return FullscreenViewAdderFingerprint.toErrorResult()

        LayoutConstructorFingerprint.result?.mutableMethod?.let {
            val instructions = it.implementation!!.instructions
            val dummyRegister = it.getInstruction<OneRegisterInstruction>(it.getStringIndex("1.0x")).registerA

            val invokeIndex = instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                        ((instruction as ReferenceInstruction).reference.toString() ==
                                "Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V")
            }

            it.addInstructionsWithLabels(
                invokeIndex, """
                    invoke-static {}, $FULLSCREEN->showFullscreenTitle()Z
                    move-result v$dummyRegister
                    if-eqz v$dummyRegister, :hidden
                """, ExternalLabel("hidden", it.getInstruction(invokeIndex + 1))
            )
        } ?: return LayoutConstructorFingerprint.toErrorResult()

        /**
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
