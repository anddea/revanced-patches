package app.revanced.patches.youtube.general.tabletminiplayer.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ModernMiniPlayerConstructorFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literalSupplier = { 45623000 }
)