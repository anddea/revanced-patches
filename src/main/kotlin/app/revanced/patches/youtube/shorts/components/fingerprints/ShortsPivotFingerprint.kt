package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelPivotButton
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.containsWideLiteralInstructionIndex
import app.revanced.util.getStringInstructionIndex

internal object ShortsPivotFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = custom@{ methodDef, _ ->
        if (!methodDef.containsWideLiteralInstructionIndex(ReelPivotButton))
            return@custom false

        if (!SettingsPatch.upward1912)
            return@custom true

        methodDef.getStringInstructionIndex("RHS is rendered through element view for Ads") > 0
    }
)