package app.revanced.patches.music.layout.sleeptimer.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.layout.sleeptimer.fingerprints.SleepTimerFingerprint
import app.revanced.patches.music.utils.settings.resource.patch.MusicSettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-sleep-timer")
@Description("Add sleep timer to flyout menu.")
@DependsOn([MusicSettingsPatch::class])
@MusicCompatibility
@Version("0.0.1")
class SleepTimerPatch : BytecodePatch(
    listOf(SleepTimerFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SleepTimerFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$targetRegister}, $MUSIC_LAYOUT->enableSleepTimer(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: return SleepTimerFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(
            CategoryType.LAYOUT,
            "revanced_enable_sleep_timer",
            "true"
        )

        return PatchResultSuccess()
    }
}