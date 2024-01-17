package app.revanced.patches.music.flyoutpanel.sleeptimer

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.flyoutpanel.sleeptimer.fingerprints.SleepTimerFingerprint
import app.revanced.patches.music.utils.integrations.Constants.FLYOUT
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Enable sleep timer",
    description = "Adds an option to add the sleep timer to the flyout menu.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object SleepTimerPatch : BytecodePatch(
    setOf(SleepTimerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        SleepTimerFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {}, $FLYOUT->enableSleepTimer()Z
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