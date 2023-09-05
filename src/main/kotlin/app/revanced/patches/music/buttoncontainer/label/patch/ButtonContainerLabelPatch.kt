package app.revanced.patches.music.buttoncontainer.label.patch

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
import app.revanced.patches.music.buttoncontainer.label.fingerprints.ButtonContainerLabelFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.fingerprints.ActionsContainerParentFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_BUTTON_CONTAINER
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide button container labels")
@Description("Hide labels in button container.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class ButtonContainerLabelPatch : BytecodePatch(
    listOf(ActionsContainerParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ActionsContainerParentFingerprint.result?.let { parentResult ->
            ButtonContainerLabelFingerprint.also {
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
                            invoke-static {v$targetRegister}, $MUSIC_BUTTON_CONTAINER->hideButtonContainerLabel(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
            } ?: throw ButtonContainerLabelFingerprint.exception
        } ?: throw ActionsContainerParentFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.BUTTON_CONTAINER,
            "revanced_hide_button_container_label",
            "false"
        )

    }
}
