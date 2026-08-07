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

package app.morphe.patches.music.navigation.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.music.utils.resourceid.colorGrey
import app.morphe.patches.music.utils.resourceid.text1
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.morphe.patches.music.utils.resourceid.ytOutlineSamples
import app.morphe.patches.music.utils.resourceid.ytOutlineYouTubeMusic
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

internal val tabLayoutFingerprint = legacyFingerprint(
    name = "tabLayoutFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("FEmusic_radio_builder"),
    literals = listOf(colorGrey)
)

/** Matches navigation tab creation across the legacy and Cairo navigation implementations. */
internal object TabLayoutTextFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L"),
    filters = listOf(
        anyInstruction(
            string("FEmusic_search"),
            string("FEsearch"),
            newInstance("Ljava/util/ArrayList;")
        ),
        resourceLiteral(ResourceType.ID, "text1"),
        methodCall(
            smali = "Landroid/view/View;->findViewById(I)Landroid/view/View;",
            location = MatchAfterWithin(5)
        ),
        checkCast(
            type = "Landroid/widget/TextView;",
            location = MatchAfterWithin(5)
        ),
        anyInstruction(opcode(Opcode.SGET_OBJECT), opcode(Opcode.IGET_OBJECT)),
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchAfterWithin(5)),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "L",
            parameters = listOf("I"),
            location = MatchAfterWithin(5)
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(name = "getVisibility")
    )
)

/** Legacy navigation tab match retained for replacement hooks that only exist before 8.51. */
internal val legacyTabLayoutTextFingerprint = legacyFingerprint(
    name = "legacyTabLayoutTextFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT
    ),
    strings = listOf("FEmusic_search"),
    literals = listOf(text1),
    customFingerprint = { method, _ ->
        indexOfMapInstruction(method) >= 0 &&
                indexOfGetVisibilityInstruction(method) >= 0 &&
                indexOfSetTextInstruction(method) >= 0
    }
)

internal fun indexOfGetVisibilityInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "getVisibility"
    }

internal fun indexOfSetTextInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setText"
    }

internal fun indexOfMapInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_INTERFACE &&
                getReference<MethodReference>()?.toString() == "Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    }

internal object ThemeMapConstructorFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, _ ->
        method.name == "<init>" && method.implementation?.instructions?.let { instructions ->
            var hasOutlineSamples = false
            var hasOutlineUpgrade = false
            for (instruction in instructions) {
                if (instruction.opcode == Opcode.CONST) {
                    val literal = (instruction as? WideLiteralInstruction)?.wideLiteral
                    if (literal == ytOutlineSamples) {
                        hasOutlineSamples = true
                    } else if (literal == ytOutlineYouTubeMusic) {
                        hasOutlineUpgrade = true
                    }
                }
            }
            hasOutlineSamples && hasOutlineUpgrade
        } == true
    }
)
