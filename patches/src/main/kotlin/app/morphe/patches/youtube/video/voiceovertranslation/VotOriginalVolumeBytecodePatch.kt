/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of https://github.com/anddea/revanced-patches/.
 *
 * The original author: https://github.com/Jav1x.
 *
 * IMPORTANT: This file is the proprietary work of https://github.com/Jav1x.
 * Any modifications, derivatives, or substantial rewrites of this file
 * must retain this copyright notice and the original author attribution
 * in the source code and version control history.
 */

package app.morphe.patches.youtube.video.voiceovertranslation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.transformation.transformInstructionsPatch
import app.morphe.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/patches/voiceovertranslation/VotOriginalVolumePatch;"

private const val AUDIO_TRACK_SET_VOLUME = "Landroid/media/AudioTrack;->setVolume(F)I"

/** Only patch DefaultAudioSink (cbt) and alternate sink (oec) — the two known setVolume(F) call sites. */
private fun isTargetMethod(classDef: ClassDef, method: Method): Boolean {
    val className = classDef.type.substringAfterLast("/").removeSuffix(";")
    return (className == "cbt" && method.name == "Q") || (className == "oec" && method.name == "t")
}

val votOriginalVolumeBytecodePatch = bytecodePatch(
    description = "votOriginalVolumeBytecodePatch"
) {
    dependsOn(voiceOverTranslationBytecodePatch)

    execute {
        transformInstructionsPatch(
            filterMap = { classDef, method, instruction, index ->
                if (!isTargetMethod(classDef, method)) return@transformInstructionsPatch null

                if (instruction.opcode != Opcode.INVOKE_VIRTUAL &&
                    instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE
                ) return@transformInstructionsPatch null

                val ref = instruction.getReference<MethodReference>() ?: return@transformInstructionsPatch null
                if (ref.toString() != AUDIO_TRACK_SET_VOLUME) return@transformInstructionsPatch null

                val volReg = when (instruction) {
                    is FiveRegisterInstruction -> {
                        if (instruction.registerCount >= 2) instruction.registerD
                        else return@transformInstructionsPatch null
                    }
                    is RegisterRangeInstruction -> {
                        if (instruction.registerCount >= 2)
                            instruction.startRegister + 1
                        else return@transformInstructionsPatch null
                    }
                    else -> return@transformInstructionsPatch null
                }
                index to volReg
            },
            transform = { mutableMethod, entry ->
                val (index, volReg) = entry
                mutableMethod.addInstructions(
                    index,
                    """
                    invoke-static { v$volReg }, $EXTENSION_CLASS_DESCRIPTOR->applyVolumeMultiplier(F)F
                    move-result v$volReg
                    """.trimIndent()
                )
            },
        )
    }
}
