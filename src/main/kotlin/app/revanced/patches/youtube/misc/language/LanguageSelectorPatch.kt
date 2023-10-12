package app.revanced.patches.youtube.misc.language

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.misc.language.fingerprints.GeneralPrefsFingerprint
import app.revanced.patches.youtube.misc.language.fingerprints.GeneralPrefsLegacyFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Language switch",
    description = "Add language switch toggle.",
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

                addInstruction(
                    targetIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        SettingsPatch.updatePatchStatus("language-switch")

    }
}
