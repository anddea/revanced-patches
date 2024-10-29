package app.revanced.patches.reddit.layout.subredditdialog.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch.CancelButton
import app.revanced.util.containsWideLiteralInstructionValue
import com.android.tools.smali.dexlib2.AccessFlags

internal object FrequentUpdatesSheetScreenFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, classDef ->
        methodDef.containsWideLiteralInstructionValue(CancelButton)
                && classDef.sourceFile == "FrequentUpdatesSheetScreen.kt"
    }
)