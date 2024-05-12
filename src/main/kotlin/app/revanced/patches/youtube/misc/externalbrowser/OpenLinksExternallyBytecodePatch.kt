package app.revanced.patches.youtube.misc.externalbrowser

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.transformation.BaseTransformInstructionsPatch
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

object OpenLinksExternallyBytecodePatch : BaseTransformInstructionsPatch<Pair<Int, Int>>(
) {
    override fun filterMap(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        instructionIndex: Int
    ): Pair<Int, Int>? {
        if (instruction !is ReferenceInstruction) return null
        val reference = instruction.reference as? StringReference ?: return null

        if (reference.string != "android.support.customtabs.action.CustomTabsService") return null

        return instructionIndex to (instruction as OneRegisterInstruction).registerA
    }

    override fun transform(mutableMethod: MutableMethod, entry: Pair<Int, Int>) {
        val (intentStringIndex, register) = entry

        // Hook the intent string.
        mutableMethod.addInstructions(
            intentStringIndex + 1, """
                invoke-static {v$register}, $MISC_PATH/ExternalBrowserPatch;->enableExternalBrowser(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v$register
                """
        )
    }
}