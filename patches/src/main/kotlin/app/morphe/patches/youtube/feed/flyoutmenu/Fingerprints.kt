/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.feed.flyoutmenu

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ContextualMenuItemBuilderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.SYNTHETIC),
    returnType = "V",
    parameters = listOf("L", "L"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "Ljava/lang/CharSequence;",
        ),
        checkCast(
            type = "Landroid/widget/TextView;",
            location = MatchAfterWithin(3),
        ),
        methodCall(
            smali = "Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V",
            location = MatchAfterWithin(5),
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "I",
            location = MatchAfterWithin(7),
        ),
        resourceLiteral(ResourceType.ATTR, "ytTextPrimary"),
    )
)

/**
 * Lists the items of a menu for the popup menus.
 */
internal object MenuItemListBuilderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("L", "Ljava/lang/Object;", "L", "L"),
    filters = listOf(
        methodCall(smali = "Ljava/util/List;->iterator()Ljava/util/Iterator;"),
        methodCall(smali = "Ljava/util/Iterator;->hasNext()Z"),
        methodCall(smali = "Ljava/util/Iterator;->next()Ljava/lang/Object;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("L", "Ljava/lang/Object;", "L", "L"),
            returnType = "L",
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(parameters = listOf("Ljava/lang/Iterable;"), location = MatchAfterImmediately())
    )
)

/**
 * Adds the items of the toolbar menu, such as the overflow menu of a playlist page.
 */
internal object ActionBarMenuBuilderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    filters = listOf(
        methodCall(smali = "Landroid/util/SparseArray;->size()I"),
        methodCall(smali = "Landroid/view/MenuInflater;->inflate(ILandroid/view/Menu;)V"),
        methodCall(smali = "Landroid/view/Menu;->add(IIILjava/lang/CharSequence;)Landroid/view/MenuItem;")
    )
)

