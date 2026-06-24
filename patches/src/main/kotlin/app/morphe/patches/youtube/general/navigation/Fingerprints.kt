/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.youtube.utils.YOUTUBE_PIVOT_BAR_CLASS_TYPE
import app.morphe.patches.youtube.utils.resourceid.actionBarSearchResultsViewMic
import app.morphe.patches.youtube.utils.resourceid.newContentCount
import app.morphe.patches.youtube.utils.resourceid.newContentDot
import app.morphe.patches.youtube.utils.resourceid.searchQuery
import app.morphe.patches.youtube.utils.resourceid.ytFillBell
import app.morphe.patches.youtube.utils.resourceid.ytOutlineLibrary
import app.morphe.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val UNKNOWN_STRING = "UNKNOWN"
internal const val SEARCH_STRING = "SEARCH"
internal const val SEARCH_CAIRO_STRING = "SEARCH_CAIRO"
internal const val TAB_ACTIVITY_STRING = "TAB_ACTIVITY"
internal const val TAB_ACTIVITY_CAIRO_STRING = "TAB_ACTIVITY_CAIRO"

internal object AnimatedNavigationTabsFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    filters = listOf(
        literal(45680008L)
    )
)

internal object ActionBarSearchResultsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    custom = { method, _ ->
        method.containsLiteralInstruction(actionBarSearchResultsViewMic) &&
                method.containsLiteralInstruction(searchQuery)
    }
)

internal object ImageEnumConstructorFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(
        UNKNOWN_STRING,
        SEARCH_STRING,
        TAB_ACTIVITY_STRING
    )
)

internal object PivotBarBuilderFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, classDef ->
        method.name == "<init>" &&
                method.containsLiteralInstruction(newContentCount) &&
                method.containsLiteralInstruction(newContentDot) &&
                classDef.fields.find { it.type.endsWith("/PivotBar;") } != null
    }
)

internal object PivotBarRendererFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L"),
    returnType = "Lj$/util/Optional;",
    filters = listOf(
        literal(117501096L),
        opcode(Opcode.IF_NE),
        opcode(Opcode.CHECK_CAST),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            definingClass = "this",
            name = "<init>",
            returnType = "V"
        ),
        opcode(Opcode.RETURN_OBJECT)
    )
)

internal object PivotBarRendererListFingerprint : Fingerprint(
    parameters = listOf("L"),
    returnType = "V",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "L"
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("L"),
            returnType = "L"
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "L"
        ),
        literal(45633821L),
    )
)

internal object PivotBarChangedFingerprint : Fingerprint(
    name = "onConfigurationChanged",
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    ),
    custom = { method, _ ->
        method.definingClass.endsWith("/PivotBar;")
    }
)

internal object PivotBarSetTextFingerprint : Fingerprint(
    name = "<init>",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(
        YOUTUBE_PIVOT_BAR_CLASS_TYPE,
        "Landroid/widget/TextView;",
        "Ljava/lang/CharSequence;"
    ),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    )
)

internal object PivotBarStyleFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.XOR_INT_2ADDR
    ),
    custom = { method, _ ->
        method.definingClass.endsWith("/PivotBar;")
    }
)

internal object TopBarRendererPrimaryFilterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT),
        checkCast(
            type = "Ljava/util/List;",
            location = MatchAfterWithin(5),
        ),
        opcode(
            opcode = Opcode.CHECK_CAST,
            location = MatchAfterWithin(3),
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            returnType = "L",
            location = MatchAfterWithin(3),
        ),
        opcode(
            opcode = Opcode.CHECK_CAST,
            location = MatchAfterWithin(5),
        ),
        literal(120823052L),
    ),
)

internal object SetEnumMapFingerprint : Fingerprint(
    custom = { method, _ ->
        method.containsLiteralInstruction(ytFillBell)
    }
)

internal object SetEnumMapSecondaryFingerprint : Fingerprint(
    custom = { method, _ ->
        method.containsLiteralInstruction(ytOutlineLibrary)
    }
)

internal const val TRANSLUCENT_NAVIGATION_BAR_FEATURE_FLAG = 45630927L

internal object TranslucentNavigationBarFingerprint : Fingerprint(
    custom = { method, _ ->
        method.containsLiteralInstruction(TRANSLUCENT_NAVIGATION_BAR_FEATURE_FLAG)
    }
)

internal object AutoHideNavigationBarFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/support/v7/widget/RecyclerView;", "I", "I"),
    filters = listOf(
        methodCall("Landroid/view/ViewConfiguration;->get(Landroid/content/Context;)Landroid/view/ViewConfiguration;"),
        methodCall("Landroid/view/ViewConfiguration;->getScaledTouchSlop()I", location = MatchAfterWithin(5))
    )
)
