package app.revanced.patches.music.player.repeat

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.player.repeat.fingerprints.RepeatTrackFingerprint
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_PLAYER
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Remember repeat state",
    description = "Remembers the state of the repeat.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ]
)
@Suppress("unused")
object RememberRepeatPatch : BytecodePatch(
    setOf(RepeatTrackFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        RepeatTrackFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $MUSIC_PLAYER->rememberRepeatState(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw RepeatTrackFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_remember_repeat_state",
            "true"
        )
    }
}
