package app.morphe.patches.youtube.player.miniplayer.startup

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_RESUMING_MINIPLAYER_ON_STARTUP
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/MiniplayerPatch;"

// YT uses "Miniplayer" without a space between 'mini' and 'player: https://support.google.com/youtube/answer/9162927.
@Suppress("unused", "SpellCheckingInspection")
val resumingMiniplayerOnStartupPatch = bytecodePatch(
    DISABLE_RESUMING_MINIPLAYER_ON_STARTUP.title,
    DISABLE_RESUMING_MINIPLAYER_ON_STARTUP.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        showMiniplayerCommandFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.instructionMatches.last().index
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->disableResumingStartupMiniPlayer(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }


        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: MINIPLAYER_COMPONENTS",
                "SETTINGS: DISABLE_RESUMING_MINIPLAYER"
            ),
            DISABLE_RESUMING_MINIPLAYER_ON_STARTUP
        )

    }
}
