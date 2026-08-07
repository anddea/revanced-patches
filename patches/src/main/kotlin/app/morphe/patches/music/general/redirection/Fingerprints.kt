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

package app.morphe.patches.music.general.redirection

import app.morphe.patcher.Fingerprint
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object NotificationLikeButtonControllerFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("NotificationLikeButtonController"),
    custom = { method, _ ->
        method.name == "<clinit>"
    }
)

internal object NotificationLikeButtonOnClickListenerFingerprint : Fingerprint(
    classFingerprint = NotificationLikeButtonControllerFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    custom = { method, _ ->
        indexOfMapInstruction(method) >= 0
    }
)

internal fun indexOfMapInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.parameterTypes?.size == 3 &&
                reference.parameterTypes[2].toString() == "Ljava/util/Map;"
    }

internal object DislikeButtonOnClickListenerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    custom = custom@{ method, classDef ->
        if (classDef.fields.count() != 7) {
            return@custom false
        }
        if (classDef.methods.count() != 5) {
            return@custom false
        }

        val interfaceMethodCount = classDef.methods.count { m ->
            AccessFlags.PUBLIC.isSet(m.accessFlags) &&
                    AccessFlags.FINAL.isSet(m.accessFlags) &&
                    m.returnType == "V" &&
                    m.parameterTypes.size == 2 &&
                    m.parameterTypes.last().toString() == "Ljava/util/Map;"
        }
        if (interfaceMethodCount != 3) {
            return@custom false
        }

        val implementation = method.implementation
            ?: return@custom false
        val instructions = implementation.instructions
        val instructionCount = instructions.count()
        if (instructionCount < 50) {
            return@custom false
        }

        method.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_INTERFACE &&
                    reference?.returnType == "V" &&
                    reference.parameterTypes.size == 1
        } >= 0
    }
)

/**
 * 6.20 - 7.25
 */
internal object DislikeButtonOnClickListenerLegacyFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/view/View;"),
    custom = { method, _ ->
        method.name == "onClick" &&
                (method.containsLiteralInstruction(53465L) || method.containsLiteralInstruction(98173L))
    }
)
