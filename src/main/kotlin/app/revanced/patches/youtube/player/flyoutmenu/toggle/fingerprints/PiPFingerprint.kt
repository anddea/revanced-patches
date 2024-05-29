package app.revanced.patches.youtube.player.flyoutmenu.toggle.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object PiPFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("menu_item_picture_in_picture"),
    customFingerprint = { _, classDef ->
        classDef.methods.count() > 5
    }
)