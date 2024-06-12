package app.revanced.patches.youtube.general.miniplayer.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("SpellCheckingInspection")
internal object MiniplayerOverrideFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("appName")
)