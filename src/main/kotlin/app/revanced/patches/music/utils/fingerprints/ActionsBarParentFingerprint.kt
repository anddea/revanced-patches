package app.revanced.patches.music.utils.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ActionsContainer
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object ActionsBarParentFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literalSupplier = { ActionsContainer }
)

