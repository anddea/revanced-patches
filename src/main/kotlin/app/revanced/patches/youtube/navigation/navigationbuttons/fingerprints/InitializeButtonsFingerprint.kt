package app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.navigation.navigationbuttons.NavigationBarHookResourcePatch
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Resolves to the class found in [PivotBarConstructorFingerprint].
 */
internal object InitializeButtonsFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf(),
    literalSupplier = { NavigationBarHookResourcePatch.imageOnlyTabResourceId }
)