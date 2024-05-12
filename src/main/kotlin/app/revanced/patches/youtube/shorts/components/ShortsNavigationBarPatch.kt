package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.youtube.shorts.components.fingerprints.BottomNavigationBarFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.RenderBottomNavigationBarFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.SetPivotBarFingerprint
import app.revanced.patches.youtube.utils.fingerprints.InitializeButtonsFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getWalkerMethod
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

object ShortsNavigationBarPatch : BytecodePatch(
    setOf(
        BottomNavigationBarFingerprint,
        InitializeButtonsFingerprint,
        RenderBottomNavigationBarFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        InitializeButtonsFingerprint.resultOrThrow().let { parentResult ->
            SetPivotBarFingerprint.also { it.resolve(context, parentResult.classDef) }.resultOrThrow().let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val register = getInstruction<OneRegisterInstruction>(startIndex).registerA

                    addInstruction(
                        startIndex + 1,
                        "invoke-static {v$register}, $SHORTS_CLASS_DESCRIPTOR->setNavigationBar(Ljava/lang/Object;)V"
                    )
                }
            }
        }

        RenderBottomNavigationBarFingerprint.resultOrThrow().let {
            val walkerMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex + 1)

            walkerMethod.addInstruction(
                0,
                "invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->hideShortsNavigationBar()V"
            )
        }

        BottomNavigationBarFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithMethodReferenceName("findViewById") + 1
                val insertRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$insertRegister}, $SHORTS_CLASS_DESCRIPTOR->hideShortsNavigationBar(Landroid/view/View;)Landroid/view/View;
                        move-result-object v$insertRegister
                        """
                )
            }
        }

    }
}