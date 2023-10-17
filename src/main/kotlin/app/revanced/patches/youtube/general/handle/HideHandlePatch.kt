package app.revanced.patches.youtube.general.handle

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.general.personalinformation.fingerprints.AccountSwitcherAccessibilityLabelFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide handle",
    description = "Hides the handle in the account switcher.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36"
            ]
        )
    ]
)
@Suppress("unused")
object HideHandlePatch : BytecodePatch(
    setOf(AccountSwitcherAccessibilityLabelFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        AccountSwitcherAccessibilityLabelFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex - 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 2, """
                        invoke-static {v$targetRegister}, $GENERAL->hideEmailAddress(I)I
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw AccountSwitcherAccessibilityLabelFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_EMAIL_ADDRESS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide handle")

    }
}
