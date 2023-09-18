package app.revanced.patches.music.actionbar.label.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.actionbar.label.fingerprints.ActionBarLabelFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.fingerprints.ActionsBarParentFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_ACTIONBAR
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide action bar label")
@Description("Hide labels in action bar.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class ActionBarLabelPatch : BytecodePatch(
    listOf(ActionsBarParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ActionsBarParentFingerprint.result?.let { parentResult ->
            ActionBarLabelFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.endIndex
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex, """
                            invoke-static {v$targetRegister}, $MUSIC_ACTIONBAR->hideActionBarLabel(Z)Z
                            move-result v$targetRegister
                            """
                    )
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
