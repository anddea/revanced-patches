package app.revanced.patches.music.flyoutpanel.playbackspeed.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.MusicMenuLikeButtons
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object FlyoutPanelLikeButtonFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    parameters = listOf("L", "Ljava/lang/Object;"),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(MusicMenuLikeButtons) }
)

