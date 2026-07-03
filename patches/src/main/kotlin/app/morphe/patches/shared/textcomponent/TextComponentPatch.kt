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

package app.morphe.patches.shared.textcomponent

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.SPANNABLE_STRING_REFERENCE
import app.morphe.patches.shared.indexOfSpannableStringInstruction
import app.morphe.patches.shared.spannableStringBuilderFingerprint
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private lateinit var spannedMethod: MutableMethod
private var spannedIndex = 0
private var spannedRegister = 0
private var spannedContextRegister = 0

private lateinit var textComponentMethod: MutableMethod
private var textComponentIndex = 0
private var textComponentRegister = 0
private var textComponentContextRegister = 0

val textComponentPatch = bytecodePatch(
    description = "textComponentPatch"
) {
    execute {
        spannableStringBuilderFingerprint.methodOrThrow().apply {
            spannedMethod = this
            spannedIndex = indexOfSpannableStringInstruction(this)
            spannedRegister = getInstruction<FiveRegisterInstruction>(spannedIndex).registerC
            spannedContextRegister =
                getInstruction<OneRegisterInstruction>(spannedIndex + 1).registerA

            replaceInstruction(
                spannedIndex,
                "move-object/from16 v$spannedContextRegister, p0"
            )
            addInstruction(
                ++spannedIndex,
                "invoke-static {v$spannedRegister}, $SPANNABLE_STRING_REFERENCE"
            )
        }

        TextComponentContextFingerprint.method.apply {
            textComponentMethod = this

            // ~ YouTube 19.32.xx
            val legacyCharSequenceIndex = indexOfFirstInstruction {
                getReference<FieldReference>()?.type == "Ljava/util/BitSet;"
            } - 1
            val charSequenceIndex = indexOfFirstInstruction {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_VIRTUAL &&
                        reference?.returnType == "V" &&
                        reference.parameterTypes.firstOrNull() == "Ljava/lang/CharSequence;"
            }

            val insertIndex: Int

            if (legacyCharSequenceIndex > -2) {
                textComponentRegister =
                    getInstruction<TwoRegisterInstruction>(legacyCharSequenceIndex).registerA
                insertIndex = legacyCharSequenceIndex - 1
            } else if (charSequenceIndex > -1) {
                textComponentRegister =
                    getInstruction<FiveRegisterInstruction>(charSequenceIndex).registerD
                insertIndex = charSequenceIndex
            } else {
                throw PatchException("Could not find insert index")
            }

            val conversionContextFieldIndex = indexOfFirstInstructionReversedOrThrow(insertIndex) {
                getReference<FieldReference>()?.type == "Ljava/util/Map;"
            }
            val conversionContextRegister = getInstruction<TwoRegisterInstruction>(
                conversionContextFieldIndex
            ).registerA

            textComponentContextRegister = conversionContextRegister
            textComponentIndex = insertIndex
        }
    }
}

internal fun hookSpannableString(
    classDescriptor: String,
    methodName: String
) = spannedMethod.addInstructions(
    spannedIndex, """
        invoke-static {v$spannedContextRegister, v$spannedRegister}, $classDescriptor->$methodName(Ljava/lang/Object;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
        move-result-object v$spannedRegister
        """
)

internal fun hookTextComponent(
    classDescriptor: String,
    methodName: String = "onLithoTextLoaded"
) = textComponentMethod.apply {
    addInstructions(
        textComponentIndex, """
            invoke-static {v$textComponentContextRegister, v$textComponentRegister}, $classDescriptor->$methodName(Ljava/lang/Object;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
            move-result-object v$textComponentRegister
            """
    )
    textComponentIndex += 2
}
