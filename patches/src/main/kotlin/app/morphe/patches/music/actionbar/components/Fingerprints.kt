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

package app.morphe.patches.music.actionbar.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.string
import app.morphe.patches.music.utils.resourceid.elementsLottieAnimationViewTagId
import app.morphe.patches.music.utils.resourceid.likeDislikeContainer
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val actionBarComponentFingerprint = legacyFingerprint(
    name = "actionBarComponentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.AND_INT_LIT16,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.SGET_OBJECT
    ),
    literals = listOf(99180L),
)

internal val likeDislikeContainerFingerprint = legacyFingerprint(
    name = "likeDislikeContainerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(likeDislikeContainer)
)

internal val lottieAnimationViewTagFingerprint = legacyFingerprint(
    name = "lottieAnimationViewTagFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    literals = listOf(elementsLottieAnimationViewTagId)
)

internal val commandResolverFingerprint = legacyFingerprint(
    name = "commandResolverFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC or AccessFlags.FINAL,
    returnType = "Z",
    parameters = listOf("L", "L", "Ljava/util/Map;"),
    strings = listOf("CommandResolver threw exception during resolution")
)

internal val offlineVideoEndpointFingerprint = legacyFingerprint(
    name = "offlineVideoEndpointFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    strings = listOf("Object is not an offlineable video: %s")
)

internal object ButtonProtoBufferGetterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "[B",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Lcom/google/android/libraries/elements/adl/UpbMessage;->jniEncode(JJ)[B"
        )
    )
)

internal object ComponentListFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        returnType = "L",
        filters = listOf(
            string("Failed to parse Element proto."),
            string("Cannot read theme key from model.")
        )
    ),
    returnType = "Ljava/util/List;",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            name = "nCopies",
        ),
    ),
)

internal object TreeNodeListFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "L",
            location = MatchAfterWithin(5)
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/util/concurrent/atomic/AtomicReference;",
            location = MatchAfterWithin(5)
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/util/concurrent/atomic/AtomicReference;",
            location = MatchAfterWithin(2)
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Ljava/util/concurrent/atomic/AtomicReference;->get()Ljava/lang/Object;",
            location = MatchAfterWithin(2)
        )
    )
)

internal object TreeNodeListHelperConstructorFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "L",
        parameters = listOf("L", "L"),
        filters = listOf(
            newInstance("Ljava/util/ArrayList;", location = MatchFirst()),
            methodCall(
                opcode = Opcode.INVOKE_DIRECT,
                smali = "Ljava/util/ArrayList;-><init>()V",
                location = MatchAfterImmediately(),
            ),
            literal(0, location = MatchAfterWithin(10))
        ),
        custom = { _, classDef ->
            classDef.methods.count() == 2
        }
    ),
    name = "<init>"
)
