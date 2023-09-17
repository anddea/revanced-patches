package app.revanced.patches.music.misc.exclusiveaudio.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object PodCastConfigFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45388403) }
)