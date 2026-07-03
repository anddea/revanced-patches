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

package app.morphe.patches.shared.customspeed

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.playservice.is_8_51_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch as musicVersionCheckPatch
import app.morphe.patches.youtube.utils.playservice.is_20_34_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch as youtubeVersionCheckPatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private var patchIncluded = false

fun customPlaybackSpeedPatch(
    descriptor: String,
    maxSpeed: Float
) = bytecodePatch(
    description = "customPlaybackSpeedPatch"
) {
    dependsOn(youtubeVersionCheckPatch, musicVersionCheckPatch)

    execute {
        if (patchIncluded) {
            return@execute
        }

        ArrayGeneratorFingerprint.apply {
            method.apply {
                val targetIndex = instructionMatches.first().index
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $descriptor->getLength(I)I
                        move-result v$targetRegister
                        """
                )

                val sizeIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "size"
                } + 1
                val sizeRegister = getInstruction<OneRegisterInstruction>(sizeIndex).registerA

                addInstructions(
                    sizeIndex + 1, """
                        invoke-static {v$sizeRegister}, $descriptor->getSize(I)I
                        move-result v$sizeRegister
                        """
                )

                val arrayIndex = indexOfFirstInstructionOrThrow {
                    getReference<FieldReference>()?.type == "[F"
                }
                val arrayRegister = getInstruction<OneRegisterInstruction>(arrayIndex).registerA

                addInstructions(
                    arrayIndex + 1, """
                        invoke-static {v$arrayRegister}, $descriptor->getArray([F)[F
                        move-result-object v$arrayRegister
                        """
                )
            }
        }

        // YouTube Music 8.51+ uses the same modern limiter contract as YouTube 20.34+.
        val useNewLimiter = is_20_34_or_greater || is_8_51_or_greater
        val limiterMethods = if (useNewLimiter) {
            setOf(LimiterFingerprint.method)
        } else {
            setOf(
                LimiterFallBackFingerprint.method,
                LimiterLegacyFingerprint.match(LimiterFallBackFingerprint.classDef).method
            )
        }

        limiterMethods.forEach { method ->
            method.apply {
                val limitMinIndex = indexOfFirstLiteralInstructionOrThrow(0.25f.toRawBits().toLong())

                val limitMaxIndex = if (useNewLimiter) {
                    indexOfFirstLiteralInstructionOrThrow(4.0f.toRawBits().toLong())
                } else {
                    indexOfFirstInstructionOrThrow(limitMinIndex + 1, Opcode.CONST_HIGH16)
                }

                val limitMinRegister = getInstruction<OneRegisterInstruction>(limitMinIndex).registerA
                val limitMaxRegister = getInstruction<OneRegisterInstruction>(limitMaxIndex).registerA

                replaceInstruction(
                    limitMinIndex,
                    "const/high16 v$limitMinRegister, 0x0"
                )
                replaceInstruction(
                    limitMaxIndex,
                    "const/high16 v$limitMaxRegister, ${maxSpeed.toRawBits()}"
                )
            }
        }

        if (is_20_34_or_greater) {
            ServerSideMaxSpeedFeatureFlagFingerprint.method.returnEarly(false)
        }

        patchIncluded = true

    }
}
