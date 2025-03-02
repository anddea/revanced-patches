package app.revanced.patches.music.account.components

import app.revanced.patches.music.utils.resourceid.accountSwitcherAccessibility
import app.revanced.patches.music.utils.resourceid.menuEntry
import app.revanced.patches.music.utils.resourceid.namesInactiveAccountThumbnailSize
import app.revanced.patches.music.utils.resourceid.tosFooter
import app.revanced.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal val accountSwitcherAccessibilityLabelFingerprint = legacyFingerprint(
    name = "accountSwitcherAccessibilityLabelFingerprint",
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    literals = listOf(accountSwitcherAccessibility)
)

internal val menuEntryFingerprint = legacyFingerprint(
    name = "menuEntryFingerprint",
    returnType = "V",
    literals = listOf(menuEntry)
)

internal val namesInactiveAccountThumbnailSizeFingerprint = legacyFingerprint(
    name = "namesInactiveAccountThumbnailSizeFingerprint",
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.GOTO,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_EQZ
    ),
    literals = listOf(namesInactiveAccountThumbnailSize)
)

internal val termsOfServiceFingerprint = legacyFingerprint(
    name = "termsOfServiceFingerprint",
    returnType = "Landroid/view/View;",
    literals = listOf(tosFooter)
)
