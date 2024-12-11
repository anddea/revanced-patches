package app.revanced.patches.music.flyoutmenu.components

import app.revanced.patches.music.utils.resourceid.endButtonsContainer
import app.revanced.patches.music.utils.resourceid.touchOutside
import app.revanced.patches.music.utils.resourceid.trimSilenceSwitch
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val endButtonsContainerFingerprint = legacyFingerprint(
    name = "endButtonsContainerFingerprint",
    returnType = "V",
    literals = listOf(endButtonsContainer)
)

internal val menuItemFingerprint = legacyFingerprint(
    name = "menuItemFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_DIRECT,
        Opcode.MOVE_RESULT_OBJECT
    ),
    strings = listOf("toggleMenuItemMutations")
)

internal val screenWidthFingerprint = legacyFingerprint(
    name = "screenWidthFingerprint",
    returnType = "Z",
    parameters = listOf("L"),
    opcodes = listOf(Opcode.IF_LT),
    literals = listOf(600L)
)

internal val screenWidthParentFingerprint = legacyFingerprint(
    name = "screenWidthParentFingerprint",
    returnType = "Landroid/graphics/Bitmap;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Landroid/app/Activity;", "I"),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstructionReversed {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "destroyDrawingCache"
        } >= 0
    }
)

internal val sleepTimerFingerprint = legacyFingerprint(
    name = "sleepTimerFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(45372767L)
)

internal val touchOutsideFingerprint = legacyFingerprint(
    name = "touchOutsideFingerprint",
    returnType = "Landroid/view/View;",
    literals = listOf(touchOutside)
)

internal val trimSilenceConfigFingerprint = legacyFingerprint(
    name = "trimSilenceConfigFingerprint",
    returnType = "Z",
    literals = listOf(45619123L)
)

internal val trimSilenceSwitchFingerprint = legacyFingerprint(
    name = "trimSilenceSwitchFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(trimSilenceSwitch)
)