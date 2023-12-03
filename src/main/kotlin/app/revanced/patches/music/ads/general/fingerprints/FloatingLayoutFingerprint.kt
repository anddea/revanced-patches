package app.revanced.patches.music.ads.general.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.FloatingLayout
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object FloatingLayoutFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(FloatingLayout) }
)