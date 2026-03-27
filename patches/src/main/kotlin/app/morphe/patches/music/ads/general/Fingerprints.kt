package app.morphe.patches.music.ads.general

import app.morphe.patches.music.utils.resourceid.buttonContainer
import app.morphe.patches.music.utils.resourceid.floatingLayout
import app.morphe.patches.music.utils.resourceid.modernDialogBackground
import app.morphe.patches.music.utils.resourceid.musicNotifierShelf
import app.morphe.patches.music.utils.resourceid.privacyTosFooter
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val accountMenuFooterFingerprint = legacyFingerprint(
    name = "accountMenuFooterFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT
    ),
    literals = listOf(privacyTosFooter)
)

internal val floatingLayoutFingerprint = legacyFingerprint(
    name = "floatingLayoutFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(floatingLayout)
)

internal val getPremiumDialogParentFingerprint = legacyFingerprint(
    name = "getPremiumDialogParentFingerprint",
    returnType = "Landroid/graphics/drawable/Drawable;",
    accessFlags = AccessFlags.PROTECTED.value,
    parameters = listOf("Landroid/content/Context;"),
    literals = listOf(modernDialogBackground)
)

internal val getPremiumDialogFingerprint = legacyFingerprint(
    name = "getPremiumDialogFingerprint",
    returnType = "Landroid/app/Dialog;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, _ ->
        indexOfSetContentViewInstruction(method) >= 0
    }
)

internal fun indexOfSetContentViewInstruction(method: Method) =
    method.indexOfFirstInstruction {
        getReference<MethodReference>()?.toString() == "Landroid/app/Dialog;->setContentView(Landroid/view/View;)V"
    }

internal val getPremiumTextViewFingerprint = legacyFingerprint(
    name = "getPremiumTextViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_BOOLEAN,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC
    ),
    strings = listOf("FEmusic_history")
)

internal val notifierShelfFingerprint = legacyFingerprint(
    name = "notifierShelfFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(musicNotifierShelf, buttonContainer)
)