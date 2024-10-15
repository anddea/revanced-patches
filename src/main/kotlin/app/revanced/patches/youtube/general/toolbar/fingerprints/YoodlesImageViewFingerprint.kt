package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.YouTubeLogo
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("SpellCheckingInspection")
internal object YoodlesImageViewFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    returnType = "Landroid/view/View;",
    literalSupplier = { YouTubeLogo }
)