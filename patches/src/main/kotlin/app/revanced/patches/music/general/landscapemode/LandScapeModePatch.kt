package app.revanced.patches.music.general.landscapemode

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.patch.PatchList.ENABLE_LANDSCAPE_MODE
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val landScapeModePatch = bytecodePatch(
    ENABLE_LANDSCAPE_MODE.title,
    ENABLE_LANDSCAPE_MODE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch,
    )

    execute {
        tabletIdentifierFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->enableLandScapeMode(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_enable_landscape_mode",
            "false"
        )

        updatePatchStatus(ENABLE_LANDSCAPE_MODE)

    }
}
