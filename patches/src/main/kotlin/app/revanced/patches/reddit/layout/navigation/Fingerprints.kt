package app.revanced.patches.reddit.layout.navigation

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val bottomNavScreenFingerprint = legacyFingerprint(
    name = "bottomNavScreenFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { method, _ ->
        method.definingClass == "Lcom/reddit/launch/bottomnav/BottomNavScreen;" &&
                indexOfGetDimensionPixelSizeInstruction(method) >= 0
    }
)

fun indexOfGetDimensionPixelSizeInstruction(methodDef: Method) =
    methodDef.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.toString() == "Landroid/content/res/Resources;->getDimensionPixelSize(I)I"
    }

internal val bottomNavScreenHandlerFingerprint = legacyFingerprint(
    name = "bottomNavScreenHandlerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L", "Z", "Landroid/view/ViewGroup;", "L"),
    customFingerprint = { method, _ ->
        indexOfGetItemsInstruction(method) >= 0 &&
                indexOfSetSelectedItemTypeInstruction(method) >= 0
    }
)

fun indexOfGetItemsInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "getItems"
    }

fun indexOfSetSelectedItemTypeInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setSelectedItemType"
    }

internal val bottomNavScreenOnGlobalLayoutFingerprint = legacyFingerprint(
    name = "bottomNavScreenOnGlobalLayoutFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "onGlobalLayout"
    }
)
