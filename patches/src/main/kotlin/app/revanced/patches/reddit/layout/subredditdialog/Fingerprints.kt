package app.revanced.patches.reddit.layout.subredditdialog

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val frequentUpdatesSheetScreenFingerprint = legacyFingerprint(
    name = "frequentUpdatesSheetScreenFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IF_EQZ
    ),
    customFingerprint = { _, classDef ->
        classDef.type == "Lcom/reddit/screens/pager/FrequentUpdatesSheetScreen;"
    }
)

internal val redditAlertDialogsFingerprint = legacyFingerprint(
    name = "redditAlertDialogsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { method, _ ->
        method.definingClass.startsWith("Lcom/reddit/screen/dialog/") &&
                indexOfSetBackgroundTintListInstruction(method) >= 0
    }
)

fun indexOfSetBackgroundTintListInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setBackgroundTintList"
    }