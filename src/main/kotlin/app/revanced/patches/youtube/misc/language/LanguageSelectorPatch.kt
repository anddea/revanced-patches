package app.revanced.patches.youtube.misc.language

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.misc.language.fingerprints.GeneralPrefsFingerprint
import app.revanced.patches.youtube.misc.language.fingerprints.GeneralPrefsLegacyFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Enable language switch",
    description = "Enable/disable language switch toggle.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
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
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43"
            ]
        )
    ]
)
@Suppress("unused")
object LanguageSelectorPatch : BytecodePatch(
    setOf(
        GeneralPrefsFingerprint,
        GeneralPrefsLegacyFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        val result = GeneralPrefsFingerprint.result // YouTube v18.33.xx ~
            ?: GeneralPrefsLegacyFingerprint.result // ~ YouTube v18.33.xx
            ?: throw GeneralPrefsFingerprint.exception

        result.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {}, $MISC_PATH/LanguageSelectorPatch;->enableLanguageSwitch()Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_LANGUAGE_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus("Enable language switch")

    }
}
