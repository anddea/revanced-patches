package app.revanced.patches.youtube.utils.navigation.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.navigation.fingerprints.InitializeBottomBarContainerFingerprint.indexOfLayoutChangeListenerInstruction
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.BottomBarContainer
import app.revanced.util.containsWideLiteralInstructionValue
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object InitializeBottomBarContainerFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, classDef ->
        AccessFlags.SYNTHETIC.isSet(classDef.accessFlags) &&
                methodDef.containsWideLiteralInstructionValue(BottomBarContainer) &&
                indexOfLayoutChangeListenerInstruction(methodDef) >= 0
    },
) {
    fun indexOfLayoutChangeListenerInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.toString() == "Landroid/view/View;->addOnLayoutChangeListener(Landroid/view/View${'$'}OnLayoutChangeListener;)V"
        }
}