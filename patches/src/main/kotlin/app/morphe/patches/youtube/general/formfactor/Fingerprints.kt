/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.formfactor

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object formFactorEnumConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "UNKNOWN_FORM_FACTOR",
        "SMALL_FORM_FACTOR",
        "LARGE_FORM_FACTOR",
        "AUTOMOTIVE_FORM_FACTOR",
    )
)

internal object widthDpUIFingerprint : Fingerprint(
    returnType = "I",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IF_NEZ, Opcode.CONST_4, Opcode.RETURN,
        Opcode.CONST_16, Opcode.IF_GE, Opcode.CONST_4, Opcode.RETURN,
        Opcode.CONST_16, Opcode.IF_GE, Opcode.CONST_4, Opcode.RETURN,
        Opcode.CONST_16, Opcode.IF_GE, Opcode.CONST_4, Opcode.RETURN,
        Opcode.CONST_4, Opcode.RETURN,
    ),
    custom = { method, _ ->
        method.containsLiteralInstruction(480L) &&
                method.containsLiteralInstruction(600L) &&
                method.containsLiteralInstruction(720L)
    }
)

internal object RepeatedItemSectionRendererFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("Number of sectionList models must be equal to the number of section states"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/util/List;",
        ),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            smali = "Ljava/util/List;->get(I)Ljava/lang/Object;",
            location = MatchAfterImmediately(),
        ),
        opcode(
            Opcode.MOVE_RESULT_OBJECT,
            location = MatchAfterImmediately(),
        ),
        opcode(
            Opcode.CHECK_CAST,
            location = MatchAfterImmediately(),
        ),
        opcode(
            Opcode.INVOKE_VIRTUAL,
            location = MatchAfterImmediately(),
        ),
        opcode(
            Opcode.INSTANCE_OF,
            location = MatchAfterImmediately(),
        ),
        opcode(
            Opcode.IF_EQZ,
            location = MatchAfterImmediately(),
        ),
        opcode(
            Opcode.CHECK_CAST,
            location = MatchAfterImmediately(),
        ),
        opcode(
            Opcode.IGET_OBJECT,
            location = MatchAfterImmediately(),
        ),
    ),
)

private object ResponsiveContainerPropertiesToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("ResponsiveContainerProperties{usingResponsiveContainerConfiguration=")
)

/**
 * Picks the responsive container properties by the width and height of a container.
 * Since 21.30 the home feed lays out its items by the size class of the picked properties.
 */
context(patchContext: BytecodePatchContext)
internal fun getResponsiveContainerPropertiesFingerprint() = object : Fingerprint(
    returnType = ResponsiveContainerPropertiesToStringFingerprint.originalClassDef.type,
    parameters = listOf("I", "I")
) {}
