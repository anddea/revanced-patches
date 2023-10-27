package app.revanced.patches.youtube.player.musicbutton.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.MusicAppDeeplinkButtonView
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object MusicAppDeeplinkButtonFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z", "Z"),
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(
            MusicAppDeeplinkButtonView
        )
    })