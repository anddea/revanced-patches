package app.revanced.patches.youtube.player.buttons.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FadeDurationFast
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FullScreenButton
import app.revanced.util.containsWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.AccessFlags

internal object FullScreenButtonFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionIndex(FadeDurationFast)
                && methodDef.containsWideLiteralInstructionIndex(FullScreenButton)
    },
)