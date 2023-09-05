package app.revanced.patches.music.general.oldstylelibraryshelf.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object BrowseIdFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("FEmusic_offline"),
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45358178) }
)