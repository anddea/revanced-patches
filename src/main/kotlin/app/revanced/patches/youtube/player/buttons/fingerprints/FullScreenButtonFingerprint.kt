package app.revanced.patches.youtube.player.buttons.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.CfFullscreenButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FadeDurationFast
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FullScreenButton
import app.revanced.util.containsWideLiteralInstructionValue
import com.android.tools.smali.dexlib2.AccessFlags

internal object FullScreenButtonFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    customFingerprint = handler@{ methodDef, _ ->
        if (!methodDef.containsWideLiteralInstructionValue(FullScreenButton))
            return@handler false

        methodDef.containsWideLiteralInstructionValue(FadeDurationFast) // YouTube 18.29.38 ~ YouTube 19.18.41
                || methodDef.containsWideLiteralInstructionValue(CfFullscreenButton) // YouTube 19.19.39 ~
    },
)