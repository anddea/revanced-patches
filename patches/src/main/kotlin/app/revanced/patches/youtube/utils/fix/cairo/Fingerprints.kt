package app.revanced.patches.youtube.utils.fix.cairo

import app.revanced.patches.youtube.utils.resourceid.settingsFragment
import app.revanced.patches.youtube.utils.resourceid.settingsFragmentCairo
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Added in YouTube v19.04.38
 *
 * When this value is TRUE, Cairo Fragment is used.
 * In this case, some of patches may be broken, so set this value to FALSE.
 */
internal const val CAIRO_FRAGMENT_FEATURE_FLAG = 45532100L

internal val cairoFragmentConfigFingerprint = legacyFingerprint(
    name = "cairoFragmentConfigFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(CAIRO_FRAGMENT_FEATURE_FLAG),
)

internal val settingsFragmentSyntheticFingerprint = legacyFingerprint(
    name = "settingsFragmentSyntheticFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(Opcode.INVOKE_VIRTUAL_RANGE),
    literals = listOf(settingsFragment, settingsFragmentCairo),
)

internal val settingsFragmentStylePrimaryFingerprint = legacyFingerprint(
    name = "settingsFragmentStylePrimaryFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/util/List;",
        "Landroidx/preference/Preference;",
        "Lj${'$'}/util/Optional;",
        "Lj${'$'}/util/Optional;",
    ),
)

internal val settingsFragmentStyleSecondaryFingerprint = legacyFingerprint(
    name = "settingsFragmentStyleSecondaryFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(
        "Ljava/util/List;",
        "Landroidx/preference/Preference;",
    ),
)