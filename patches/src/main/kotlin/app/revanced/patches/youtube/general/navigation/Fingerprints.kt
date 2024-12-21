package app.revanced.patches.youtube.general.navigation

import app.revanced.patches.youtube.utils.resourceid.ytFillBell
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val ANDROID_AUTOMOTIVE_STRING = "Android Automotive"
internal const val TAB_ACTIVITY_CAIRO_STRING = "TAB_ACTIVITY_CAIRO"

internal val autoMotiveFingerprint = legacyFingerprint(
    name = "autoMotiveFingerprint",
    opcodes = listOf(
        Opcode.GOTO,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ
    ),
    strings = listOf(ANDROID_AUTOMOTIVE_STRING)
)

internal val imageEnumConstructorFingerprint = legacyFingerprint(
    name = "imageEnumConstructorFingerprint",
    returnType = "V",
    strings = listOf(TAB_ACTIVITY_CAIRO_STRING)
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
        "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
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

internal val setEnumMapFingerprint = legacyFingerprint(
    name = "setEnumMapFingerprint",
    literals = listOf(ytFillBell),
)

internal const val TRANSLUCENT_NAVIGATION_BUTTONS_FEATURE_FLAG = 45630927L

internal val translucentNavigationButtonsFeatureFlagFingerprint = legacyFingerprint(
    name = "translucentNavigationButtonsFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    literals = listOf(TRANSLUCENT_NAVIGATION_BUTTONS_FEATURE_FLAG)
)

/**
 * The device on screen back/home/recent buttons.
 */
internal const val TRANSLUCENT_NAVIGATION_BUTTONS_SYSTEM_FEATURE_FLAG = 45632194L

internal val translucentNavigationButtonsSystemFeatureFlagFingerprint = legacyFingerprint(
    name = "translucentNavigationButtonsSystemFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    literals = listOf(TRANSLUCENT_NAVIGATION_BUTTONS_SYSTEM_FEATURE_FLAG)
)