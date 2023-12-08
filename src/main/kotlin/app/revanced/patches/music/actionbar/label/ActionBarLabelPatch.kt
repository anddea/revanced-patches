package app.revanced.patches.music.actionbar.label

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.actionbar.label.fingerprints.ActionBarLabelFingerprint
import app.revanced.patches.music.utils.fingerprints.ActionsBarParentFingerprint
import app.revanced.patches.music.utils.integrations.Constants.ACTIONBAR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(
    name = "Hide action bar label",
    description = "Hide labels in action bar.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object ActionBarLabelPatch : BytecodePatch(
    setOf(ActionsBarParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ActionsBarParentFingerprint.result?.classDef?.let { parentClassDef ->
            ActionBarLabelFingerprint.resolve(context, parentClassDef)
            ActionBarLabelFingerprint.result?.let {
                it.mutableMethod.apply {
                    val instructions = implementation!!.instructions

                    val noLabelIndex = instructions.indexOfFirst { instruction ->
                        val reference = (instruction as? ReferenceInstruction)?.reference.toString()
                        instruction.opcode == Opcode.INVOKE_DIRECT
                                && reference.endsWith("<init>(Landroid/content/Context;)V")
                                && !reference.contains("Lcom/google/android/libraries/youtube/common/ui/YouTubeButton;")
                    } - 2

                    val replaceIndex = instructions.indexOfFirst { instruction ->
                        val reference = (instruction as? ReferenceInstruction)?.reference.toString()
                        instruction.opcode == Opcode.INVOKE_DIRECT
                                && reference.endsWith("Lcom/google/android/libraries/youtube/common/ui/YouTubeButton;-><init>(Landroid/content/Context;)V")
                    } - 2
                    val replaceInstruction = getInstruction<TwoRegisterInstruction>(replaceIndex)
                    val replaceReference = getInstruction<ReferenceInstruction>(replaceIndex).reference

                    addInstructionsWithLabels(
                        replaceIndex + 1, """
                            invoke-static {}, $ACTIONBAR->hideActionBarLabel()Z
                            move-result v${replaceInstruction.registerA}
                            if-nez v${replaceInstruction.registerA}, :hidden
                            iget-object v${replaceInstruction.registerA}, v${replaceInstruction.registerB}, $replaceReference
                            """, ExternalLabel("hidden", getInstruction(noLabelIndex))
                    )
                    removeInstruction(replaceIndex)
                }
            } ?: throw ActionBarLabelFingerprint.exception
        } ?: throw ActionsBarParentFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_bar_label",
            "false"
        )

    }
}
