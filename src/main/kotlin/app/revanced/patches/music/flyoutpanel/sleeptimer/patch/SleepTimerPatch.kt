package app.revanced.patches.music.flyoutpanel.sleeptimer.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.flyoutpanel.sleeptimer.fingerprints.SleepTimerFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_FLYOUT
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Enable sleep timer")
@Description("Add sleep timer to flyout menu.")
@DependsOn([SettingsPatch::class])
@MusicCompatibility
class SleepTimerPatch : BytecodePatch(
    listOf(SleepTimerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        SleepTimerFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {}, $MUSIC_FLYOUT->enableSleepTimer()Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw SleepTimerFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.FLYOUT,
            "revanced_enable_sleep_timer",
            "true"
        )

    }
}