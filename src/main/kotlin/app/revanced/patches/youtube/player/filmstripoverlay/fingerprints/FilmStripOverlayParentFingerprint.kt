package app.revanced.patches.youtube.player.filmstripoverlay.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.Scrubbing
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags

object FilmStripOverlayParentFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(Scrubbing) }
)