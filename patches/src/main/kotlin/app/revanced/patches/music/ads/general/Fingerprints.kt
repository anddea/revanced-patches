package app.revanced.patches.music.ads.general

import app.revanced.patches.music.utils.resourceid.buttonContainer
import app.revanced.patches.music.utils.resourceid.floatingLayout
import app.revanced.patches.music.utils.resourceid.interstitialsContainer
import app.revanced.patches.music.utils.resourceid.musicNotifierShelf
import app.revanced.patches.music.utils.resourceid.privacyTosFooter
import app.revanced.patches.music.utils.resourceid.slidingDialogAnimation
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

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

internal val interstitialsContainerFingerprint = legacyFingerprint(
    name = "interstitialsContainerFingerprint",
    returnType = "V",
    strings = listOf("overlay_controller_param"),
    literals = listOf(interstitialsContainer)
)

internal val notifierShelfFingerprint = legacyFingerprint(
    name = "notifierShelfFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(musicNotifierShelf, buttonContainer)
)

internal val showDialogCommandFingerprint = legacyFingerprint(
    name = "showDialogCommandFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.IF_EQ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET, // get dialog code
    ),
    literals = listOf(slidingDialogAnimation),
    // 6.26 and earlier has a different first parameter.
    // Since this fingerprint is somewhat weak, work around by checking for both method parameter signatures.
    customFingerprint = custom@{ method, _ ->
        // 6.26 and earlier parameters are: "L", "L"
        // 6.27+ parameters are "[B", "L"
        val parameterTypes = method.parameterTypes

        parameterTypes.size == 2 && parameterTypes[1].startsWith("L")
    },
)