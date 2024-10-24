package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.BottomBarContainer
import app.revanced.util.containsWideLiteralInstructionValue
import app.revanced.util.fingerprint.MultiMethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object BottomBarContainerHeightFingerprint : MultiMethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
    strings = listOf("r_pfvc"),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(BottomBarContainer)
    },
)