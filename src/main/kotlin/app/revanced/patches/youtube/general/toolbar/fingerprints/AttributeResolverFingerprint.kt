package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object AttributeResolverFingerprint : MethodFingerprint(
    returnType = "Landroid/graphics/drawable/Drawable;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Landroid/content/Context;", "I"),
    strings = listOf("Type of attribute is not a reference to a drawable (attr = %d, value = %s)")
)