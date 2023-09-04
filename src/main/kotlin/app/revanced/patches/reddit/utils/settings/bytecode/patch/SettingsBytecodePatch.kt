package app.revanced.patches.reddit.utils.settings.bytecode.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.reddit.utils.settings.bytecode.fingerprints.AcknowledgementsLabelBuilderFingerprint
import app.revanced.patches.reddit.utils.settings.bytecode.fingerprints.OssLicensesMenuActivityOnCreateFingerprint
import app.revanced.patches.reddit.utils.settings.bytecode.fingerprints.SettingsStatusLoadFingerprint
import app.revanced.util.bytecode.getStringIndex
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

class SettingsBytecodePatch : BytecodePatch(
    listOf(
        AcknowledgementsLabelBuilderFingerprint,
        OssLicensesMenuActivityOnCreateFingerprint,
        SettingsStatusLoadFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Replace settings label
         */
        AcknowledgementsLabelBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex =
                    getStringIndex("resources.getString(R.st\u2026g.label_acknowledgements)")
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstruction(
                    insertIndex,
                    "const-string v$insertRegister, \"ReVanced Extended\""
                )
            }
        } ?: throw AcknowledgementsLabelBuilderFingerprint.exception

        /**
         * Initialize settings activity
         */
        OssLicensesMenuActivityOnCreateFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex + 1

                addInstructions(
                    insertIndex, """
                        invoke-static {p0}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        return-void
                        """
                )
            }
        } ?: throw OssLicensesMenuActivityOnCreateFingerprint.exception

        settingsMethod = SettingsStatusLoadFingerprint.result?.mutableMethod
            ?: throw SettingsStatusLoadFingerprint.exception

    }

    internal companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/settingsmenu/ReVancedSettingActivity;->initializeSettings(Landroid/app/Activity;)V"

        private lateinit var settingsMethod: MutableMethod

        fun updateSettingsStatus(description: String) {
            settingsMethod.apply {
                addInstruction(
                    0,
                    "invoke-static {}, Lapp/revanced/reddit/settingsmenu/SettingsStatus;->$description()V"
                )
            }
        }
    }
}