package app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.navigation.navigationbuttons.NavigationBarHookResourcePatch
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ActionBarSearchResultsFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Landroid/view/View;",
    parameters = listOf("Landroid/view/LayoutInflater;"),
    literalSupplier = { NavigationBarHookResourcePatch.actionBarSearchResultsViewMicId }
)