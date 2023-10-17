package app.revanced.patches.music.utils.settings

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.fingerprints.NewPlayerLayoutFingerprint
import app.revanced.patches.music.utils.integrations.IntegrationsPatch
import app.revanced.patches.music.utils.settings.fingerprints.PreferenceFingerprint
import app.revanced.patches.music.utils.settings.fingerprints.SettingsHeadersFragmentFingerprint
import app.revanced.util.bytecode.BytecodeHelper.injectInit
import app.revanced.util.integrations.Constants.MUSIC_INTEGRATIONS_PATH
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    dependencies = [IntegrationsPatch::class],
    requiresIntegrations = true
)
object SettingsBytecodePatch : BytecodePatch(
    setOf(
        NewPlayerLayoutFingerprint,
        PreferenceFingerprint,
        SettingsHeadersFragmentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        NewPlayerLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        } ?: throw NewPlayerLayoutFingerprint.exception

        SettingsHeadersFragmentFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setActivity(Ljava/lang/Object;)V"
                )
            }
        } ?: throw SettingsHeadersFragmentFingerprint.exception

        PreferenceFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val keyRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerD
                val valueRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerE

                addInstruction(
                    targetIndex,
                    "invoke-static {v$keyRegister, v$valueRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->onPreferenceChanged(Ljava/lang/String;Z)V"
                )
            }
        } ?: throw PreferenceFingerprint.exception

        context.injectInit("InitializationPatch", "setDeviceInformation", false)
        context.injectInit("InitializationPatch", "initializeReVancedSettings", false)

    }

    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MUSIC_INTEGRATIONS_PATH/settingsmenu/SharedPreferenceChangeListener;"
}
