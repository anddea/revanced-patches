package app.revanced.patches.youtube.misc.returnyoutubedislike.oldlayout.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.slimMetadataToggleButtonId
import app.revanced.util.bytecode.isWideLiteralExists

object SlimMetadataButtonParentFingerprint : MethodFingerprint(
    returnType = "I",
    customFingerprint = { it.isWideLiteralExists(slimMetadataToggleButtonId) }
)