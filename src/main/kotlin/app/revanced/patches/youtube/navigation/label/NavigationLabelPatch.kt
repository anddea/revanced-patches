package app.revanced.patches.youtube.navigation.label

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.navigation.label.fingerprints.PivotBarSetTextFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch(
    name = "Hide navigation label",
    description = "Hide navigation bar labels.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.22.37",
                "18.23.36",
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
                "18.36.39"
            ]
        )
    ]
)
@Suppress("unused")
object NavigationLabelPatch : BytecodePatch(
    setOf(PivotBarSetTextFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PivotBarSetTextFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex - 2
                val targetReference =
                    getInstruction<ReferenceInstruction>(targetIndex).reference.toString()
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                if (targetReference != "Landroid/widget/TextView;")
                    throw PivotBarSetTextFingerprint.exception

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $NAVIGATION->hideNavigationLabel(Landroid/widget/TextView;)V"
                )
            }
        } ?: throw PivotBarSetTextFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_NAVIGATION_LABEL"
            )
        )

        SettingsPatch.updatePatchStatus("hide-navigation-label")

    }
}
