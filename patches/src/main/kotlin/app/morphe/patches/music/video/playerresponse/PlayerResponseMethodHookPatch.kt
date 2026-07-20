/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.music.video.playerresponse

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.playservice.is_7_03_or_greater
import app.morphe.patches.music.utils.playservice.is_8_51_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch

private val hooks = mutableSetOf<Hook>()

fun addPlayerResponseMethodHook(hook: Hook) {
    hooks += hook
}

private const val REGISTER_VIDEO_ID = "p1"
private const val REGISTER_PLAYER_PARAMETER = "p3"
private const val REGISTER_PLAYLIST_ID = "p4"
private const val REGISTER_PLAYLIST_INDEX = "p5"

private lateinit var playerResponseMethod: MutableMethod
private var numberOfInstructionsAdded = 0

val playerResponseMethodHookPatch = bytecodePatch(
    description = "playerResponseMethodHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    execute {
        val fingerprint: Fingerprint = if (is_8_51_or_greater) {
            ModernPlayerParameterBuilderFingerprint
        } else if (is_7_03_or_greater) {
            PlayerParameterBuilderFingerprint
        } else {
            PlayerParameterBuilderLegacyFingerprint
        }
        playerResponseMethod = fingerprint.method
    }

    finalize {
        fun hookVideoId(hook: Hook) {
            playerResponseMethod.addInstruction(
                0,
                "invoke-static {$REGISTER_VIDEO_ID}, $hook",
            )
            numberOfInstructionsAdded++
        }

        fun hookVideoIdAndPlaylistId(hook: Hook) {
            playerResponseMethod.addInstruction(
                0,
                "invoke-static {$REGISTER_VIDEO_ID, $REGISTER_PLAYLIST_ID, $REGISTER_PLAYLIST_INDEX}, $hook",
            )
            numberOfInstructionsAdded++
        }

        fun hookPlayerParameter(hook: Hook) {
            playerResponseMethod.addInstructions(
                0,
                """
                    invoke-static {$REGISTER_VIDEO_ID, v0}, $hook
                    move-result-object v0
                """,
            )
            numberOfInstructionsAdded += 2
        }

        // Reverse the order in order to preserve insertion order of the hooks.
        val beforeVideoIdHooks =
            hooks.filterIsInstance<Hook.ProtoBufferParameterBeforeVideoId>().asReversed()
        val videoIdHooks = hooks.filterIsInstance<Hook.VideoId>().asReversed()
        val videoIdAndPlaylistIdHooks =
            hooks.filterIsInstance<Hook.VideoIdAndPlaylistId>().asReversed()
        val afterVideoIdHooks = hooks.filterIsInstance<Hook.PlayerParameter>().asReversed()

        // Add the hooks in this specific order as they insert instructions at the beginning of the method.
        afterVideoIdHooks.forEach(::hookPlayerParameter)
        videoIdAndPlaylistIdHooks.forEach(::hookVideoIdAndPlaylistId)
        videoIdHooks.forEach(::hookVideoId)
        beforeVideoIdHooks.forEach(::hookPlayerParameter)

        playerResponseMethod.apply {
            addInstruction(
                0,
                "move-object/from16 v0, $REGISTER_PLAYER_PARAMETER"
            )
            numberOfInstructionsAdded++

            // Move the modified register back.
            addInstruction(
                numberOfInstructionsAdded,
                "move-object/from16 $REGISTER_PLAYER_PARAMETER, v0"
            )
        }
    }
}

sealed class Hook(private val methodDescriptor: String) {
    class VideoId(methodDescriptor: String) : Hook(methodDescriptor)
    class VideoIdAndPlaylistId(methodDescriptor: String) : Hook(methodDescriptor)

    class PlayerParameter(methodDescriptor: String) : Hook(methodDescriptor)
    class ProtoBufferParameterBeforeVideoId(methodDescriptor: String) : Hook(methodDescriptor)

    override fun toString() = methodDescriptor
}
