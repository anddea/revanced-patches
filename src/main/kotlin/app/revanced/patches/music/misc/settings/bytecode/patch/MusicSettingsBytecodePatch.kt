package app.revanced.patches.music.misc.settings.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.settings.bytecode.fingerprints.*
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.integrations.Constants.MUSIC_INTEGRATIONS_PATH
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("music-settings-bytecode-patch")
@DependsOn([MusicIntegrationsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicSettingsBytecodePatch : BytecodePatch(
    listOf(
        PreferenceFingerprint,
        SettingsHeadersFragmentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SettingsHeadersFragmentFingerprint.result?.let {
            with(it.mutableMethod) {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = (instruction(targetIndex) as OneRegisterInstruction).registerA
                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setActivity(Ljava/lang/Object;)V"
                )
            }
        } ?: return SettingsHeadersFragmentFingerprint.toErrorResult()

        PreferenceFingerprint.result?.let {
            with(it.mutableMethod) {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val keyRegister = (instruction(targetIndex) as FiveRegisterInstruction).registerD
                val valueRegister = (instruction(targetIndex) as FiveRegisterInstruction).registerE
                addInstruction(
                    targetIndex,
                    "invoke-static {v$keyRegister, v$valueRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->onPreferenceChanged(Ljava/lang/String;Z)V"
                )
            }
        } ?: return PreferenceFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
    companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$MUSIC_INTEGRATIONS_PATH/settingsmenu/SharedPreferenceChangeListener;"
    }
}
