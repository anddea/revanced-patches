package app.morphe.patches.youtube.general.navigation

import app.morphe.patches.youtube.utils.YOUTUBE_PIVOT_BAR_CLASS_TYPE
import app.morphe.patches.youtube.utils.resourceid.actionBarSearchResultsViewMic
import app.morphe.patches.youtube.utils.resourceid.newContentCount
import app.morphe.patches.youtube.utils.resourceid.newContentDot
import app.morphe.patches.youtube.utils.resourceid.searchBox
import app.morphe.patches.youtube.utils.resourceid.searchQuery
import app.morphe.patches.youtube.utils.resourceid.youTubeLogo
import app.morphe.patches.youtube.utils.resourceid.ytFillBell
import app.morphe.patches.youtube.utils.resourceid.ytOutlineLibrary
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val UNKNOWN_STRING = "UNKNOWN"
internal const val SEARCH_STRING = "SEARCH"
internal const val SEARCH_CAIRO_STRING = "SEARCH_CAIRO"
internal const val TAB_ACTIVITY_STRING = "TAB_ACTIVITY"
internal const val TAB_ACTIVITY_CAIRO_STRING = "TAB_ACTIVITY_CAIRO"

internal val actionBarSearchResultsFingerprint = legacyFingerprint(
    name = "actionBarSearchResultsFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Landroid/view/View;",
    literals = listOf(actionBarSearchResultsViewMic, searchQuery),
)

internal val imageEnumConstructorFingerprint = legacyFingerprint(
    name = "imageEnumConstructorFingerprint",
    returnType = "V",
    strings = listOf(
        UNKNOWN_STRING,
        SEARCH_STRING,
        TAB_ACTIVITY_STRING
    )
)

internal val pivotBarBuilderFingerprint = legacyFingerprint(
    name = "pivotBarBuilderFingerprint",
    returnType = "V",
    literals = listOf(newContentCount, newContentDot),
    customFingerprint = { method, classDef ->
        method.name == "<init>" &&
                classDef.fields.find { it.type.endsWith("/PivotBar;") } != null
    }
)

internal val pivotBarChangedFingerprint = legacyFingerprint(
    name = "pivotBarChangedFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/PivotBar;")
                && method.name == "onConfigurationChanged"
    }
)

internal val pivotBarSetTextFingerprint = legacyFingerprint(
    name = "pivotBarSetTextFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf(
        YOUTUBE_PIVOT_BAR_CLASS_TYPE,
        "Landroid/widget/TextView;",
        "Ljava/lang/CharSequence;"
    ),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, _ -> method.name == "<init>" }
)

internal val pivotBarStyleFingerprint = legacyFingerprint(
    name = "pivotBarStyleFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.XOR_INT_2ADDR
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/PivotBar;")
    }
)

// 19.37 ~
internal val searchBarOnClickListenerFingerprint = legacyFingerprint(
    name = "searchBarOnClickListenerFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    literals = listOf(searchBox, youTubeLogo),
)

// ~ 19.36
internal val searchBarOnClickListenerLegacyFingerprint = legacyFingerprint(
    name = "searchBarOnClickListenerLegacyFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;", "L", "Z", "Z"),
    literals = listOf(searchBox, youTubeLogo),
)

internal val setEnumMapFingerprint = legacyFingerprint(
    name = "setEnumMapFingerprint",
    literals = listOf(ytFillBell),
)

internal val setEnumMapSecondaryFingerprint = legacyFingerprint(
    name = "setEnumMapSecondaryFingerprint",
    literals = listOf(ytOutlineLibrary),
)

internal const val TRANSLUCENT_NAVIGATION_BAR_FEATURE_FLAG = 45630927L

internal val translucentNavigationBarFingerprint = legacyFingerprint(
    name = "translucentNavigationBarFingerprint",
    literals = listOf(TRANSLUCENT_NAVIGATION_BAR_FEATURE_FLAG),
)