package app.revanced.patches.reddit.layout.subredditdialog

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionReversed
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

internal val frequentUpdatesSheetV2ScreenFingerprint = legacyFingerprint(
    name = "frequentUpdatesSheetV2ScreenFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("subreddit_name"),
    customFingerprint = { method, classDef ->
        classDef.type == "Lcom/reddit/screens/pager/v2/FrequentUpdatesSheetV2Screen;"
    }
)

internal val frequentUpdatesSheetV2ScreenInvokeFingerprint = legacyFingerprint(
    name = "frequentUpdatesSheetV2ScreenInvokeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
    ),
    customFingerprint = { method, classDef ->
        classDef.type.startsWith("Lcom/reddit/screens/pager/v2/FrequentUpdatesSheetV2Screen${'$'}SheetContent${'$'}") &&
                method.name == "invoke" &&
                indexOfDismissScreenInstruction(method) >= 0
    }
)

fun indexOfDismissScreenInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.returnType == "V" &&
                reference.parameterTypes.isEmpty()
    }

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