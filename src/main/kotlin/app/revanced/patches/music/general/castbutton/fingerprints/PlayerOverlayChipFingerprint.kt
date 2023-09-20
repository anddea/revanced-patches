package app.revanced.patches.music.general.castbutton.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.PlayerOverlayChip
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object PlayerOverlayChipFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(PlayerOverlayChip) }
)
