package app.revanced.patches.youtube.utils.navbarindex.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.utils.fingerprints.OnBackPressedFingerprint
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.NavBarBuilderFingerprint
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.TopBarButtonFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.bytecode.BytecodeHelper.injectInit
import app.revanced.util.integrations.Constants.PATCHES_PATH
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@DependsOn(
    [
        SharedResourceIdPatch::class,
        LithoFilterPatch::class
    ]
)
class NavBarIndexHookPatch : BytecodePatch(
    listOf(
        NavBarBuilderFingerprint,
        OnBackPressedFingerprint,
        TopBarButtonFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        OnBackPressedFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->setLastNavBarIndex()V"
                )
            }
        } ?: throw OnBackPressedFingerprint.exception

        TopBarButtonFingerprint.injectIndex(0)

        NavBarBuilderFingerprint.result?.let {
            val endIndex = it.scanResult.patternScanResult!!.endIndex
            val onClickListener =
                it.mutableMethod.getInstruction<ReferenceInstruction>(endIndex).reference.toString()

            val targetMethod =
                context.findClass(onClickListener)?.mutableClass?.methods?.first { method -> method.name == "onClick" }

            targetMethod?.apply {
                for ((index, instruction) in implementation!!.instructions.withIndex()) {
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) continue

                    val invokeInstruction = instruction as Instruction35c
                    if ((invokeInstruction.reference as MethodReference).name != "indexOf") continue

                    val targetIndex = index + 2
                    if (getInstruction(targetIndex).opcode != Opcode.INVOKE_VIRTUAL) continue

                    val targetRegister = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    addInstruction(
                        targetIndex,
                        "invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setCurrentNavBarIndex(I)V"
                    )
                    break
                }
            }
        } ?: throw NavBarBuilderFingerprint.exception

        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/NavBarIndexFilter;")

        context.injectInit("NavBarIndexPatch", "initializeIndex", true)

    }

    companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$UTILS_PATH/NavBarIndexPatch;"

        fun MethodFingerprint.injectIndex(index: Int) {
            result?.let {
                it.mutableMethod.apply {
                    addInstructions(
                        0, """
                        const/4 v0, 0x$index
                        invoke-static {v0}, $INTEGRATIONS_CLASS_DESCRIPTOR->setCurrentNavBarIndex(I)V
                        """
                    )
                }
            } ?: throw exception
        }
    }
}