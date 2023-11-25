package app.revanced.patches.music.actionbar.label

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.actionbar.label.fingerprints.ActionBarLabelFingerprint
import app.revanced.patches.music.utils.fingerprints.ActionsBarParentFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_ACTIONBAR
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide action bar label",
    description = "Hide labels in action bar.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ]
)
@Suppress("unused")
object ActionBarLabelPatch : BytecodePatch(
    setOf(ActionsBarParentFingerprint)
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
