package app.revanced.patches.reddit.utils.settings

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.reddit.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch.LabelAcknowledgements
import app.revanced.patches.reddit.utils.settings.fingerprints.AcknowledgementsLabelBuilderFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.OssLicensesMenuActivityOnCreateFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.SettingsStatusLoadFingerprint
import app.revanced.patches.shared.settings.fingerprints.SharedSettingFingerprint
import app.revanced.util.getTargetIndex
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(dependencies = [SharedResourceIdPatch::class])
object SettingsBytecodePatch : BytecodePatch(
    setOf(
        AcknowledgementsLabelBuilderFingerprint,
        OssLicensesMenuActivityOnCreateFingerprint,
        SharedSettingFingerprint,
        SettingsStatusLoadFingerprint
    )
) {
    private const val INTEGRATIONS_METHOD_DESCRIPTOR =
        "$INTEGRATIONS_PATH/settings/ActivityHook;->initialize(Landroid/app/Activity;)V"

    private lateinit var settingsStatusLoadMethod: MutableMethod

    internal fun updateSettingsStatus(description: String) {
        settingsStatusLoadMethod.addInstruction(
            0,
            "invoke-static {}, $INTEGRATIONS_PATH/settings/SettingsStatus;->$description()V"
        )
    }

    override fun execute(context: BytecodeContext) {

        /**
         * Set SharedPrefCategory
         */
        SharedSettingFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val stringIndex = getTargetIndex(Opcode.CONST_STRING)
                val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

                replaceInstruction(
                    stringIndex,
                    "const-string v$stringRegister, \"reddit_revanced\""
                )
            }
        }

        /**
         * Replace settings label
         */
        AcknowledgementsLabelBuilderFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex =
                    getWideLiteralInstructionIndex(LabelAcknowledgements) + 3
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstruction(
                    insertIndex,
                    "const-string v$insertRegister, \"ReVanced Extended\""
                )
            }
        }

        /**
         * Initialize settings activity
         */
        OssLicensesMenuActivityOnCreateFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex + 1

                addInstructions(
                    insertIndex, """
                        invoke-static {p0}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        return-void
                        """
                )
            }
        }

        settingsStatusLoadMethod = SettingsStatusLoadFingerprint.resultOrThrow().mutableMethod
    }
}