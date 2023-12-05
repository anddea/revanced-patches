package app.revanced.patches.youtube.misc.externalbrowser

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.misc.externalbrowser.fingerprints.ExternalBrowserPrimaryFingerprint
import app.revanced.patches.youtube.misc.externalbrowser.fingerprints.ExternalBrowserSecondaryFingerprint
import app.revanced.patches.youtube.misc.externalbrowser.fingerprints.ExternalBrowserTertiaryFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getStringInstructionIndex
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Enable external browser",
    description = "Open url outside the app in an external browser.",
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
                "18.45.43",
                "18.46.43"
            ]
        )
    ]
)
@Suppress("unused")
object ExternalBrowserPatch : BytecodePatch(
    setOf(
        ExternalBrowserPrimaryFingerprint,
        ExternalBrowserSecondaryFingerprint,
        ExternalBrowserTertiaryFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        arrayOf(
            ExternalBrowserPrimaryFingerprint,
            ExternalBrowserSecondaryFingerprint,
            ExternalBrowserTertiaryFingerprint
        ).forEach { fingerprint ->
            fingerprint.result?.let {
                it.mutableMethod.apply {
                    val targetIndex =
                        getStringInstructionIndex("android.support.customtabs.action.CustomTabsService")
                    val register = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$register}, $MISC_PATH/ExternalBrowserPatch;->enableExternalBrowser(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$register
                            """
                    )
                }
            } ?: throw fingerprint.exception
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_EXTERNAL_BROWSER"
            )
        )

        SettingsPatch.updatePatchStatus("Enable external browser")

    }
}