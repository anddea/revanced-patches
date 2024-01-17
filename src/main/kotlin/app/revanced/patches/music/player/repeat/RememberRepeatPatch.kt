package app.revanced.patches.music.player.repeat

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.player.repeat.fingerprints.RepeatTrackFingerprint
import app.revanced.patches.music.utils.integrations.Constants.PLAYER
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Remember repeat state",
    description = "Adds an option to remember the state of the repeat toggle.",
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
                        invoke-static {v$targetRegister}, $PLAYER->rememberRepeatState(Z)Z
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
