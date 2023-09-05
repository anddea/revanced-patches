package app.revanced.patches.youtube.utils.quickactions.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.QuickActionsElementContainer
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object QuickActionsElementFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(
            QuickActionsElementContainer
        )
    }
)