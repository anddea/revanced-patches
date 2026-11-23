/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

@file:Suppress("SpellCheckingInspection")

package app.morphe.patches.youtube.player.miniplayer.startup

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.findFreeRegister
import app.morphe.patches.youtube.utils.playservice.is_21_42_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_43_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_RESUMING_MINIPLAYER_ON_STARTUP
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/MiniplayerPatch;"

// YT uses "Miniplayer" without a space between 'mini' and 'player: https://support.google.com/youtube/answer/9162927.
@Suppress("unused", "SpellCheckingInspection")
val resumingMiniplayerOnStartupPatch = bytecodePatch(
    DISABLE_RESUMING_MINIPLAYER_ON_STARTUP.title,
    DISABLE_RESUMING_MINIPLAYER_ON_STARTUP.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(settingsPatch, versionCheckPatch)

    execute {

        // Only skip the resume branch of the command. Other show miniplayer commands,
        // such as "Play next in queue" from the feed when no video is open, still work.
        (if (is_21_43_or_greater) {
            ShowMiniplayerCommandFingerprint to 2
        } else if (is_21_42_or_greater) {
            ShowMiniplayerCommandLegacy2142Fingerprint to 1
        } else {
            ShowMiniplayerCommandLegacyFingerprint to 3
        }).let { (fingerprint, matchIndex) ->
            fingerprint.method.apply {
                val index = fingerprint.instructionMatches[matchIndex].index + 1
                val free = findFreeRegister(index)

                addInstructionsWithLabels(
                    index,
                    """
                        invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->disableResumingStartupMiniPlayer()Z
                        move-result v$free
                        if-eqz v$free, :show_resuming_miniplayer
                        return-void
                        :show_resuming_miniplayer
                        nop
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
