package app.revanced.patches.shared.ads

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.ads.fingerprints.MusicAdsFingerprint
import app.revanced.patches.shared.ads.fingerprints.VideoAdsFingerprint
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

abstract class BaseAdsPatch(
    private val classDescriptor: String,
    private val methodDescriptor: String
) : BytecodePatch(
    setOf(
        MusicAdsFingerprint,
        VideoAdsFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        MusicAdsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = indexOfFirstInstruction {
                    val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                    opcode == Opcode.INVOKE_VIRTUAL
                            && reference?.returnType == "V"
                            && reference.parameterTypes.size == 1
                            && reference.parameterTypes.first() == "Z"
                }

                getWalkerMethod(context, targetIndex)
                    .addInstructions(
                        0, """
                            invoke-static {p1}, $classDescriptor->$methodDescriptor(Z)Z
                            move-result p1
                            """
                    )
            }
        }

        VideoAdsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $classDescriptor->$methodDescriptor()Z
                        move-result v0
                        if-nez v0, :show_ads
                        return-void
                        """, ExternalLabel("show_ads", getInstruction(0))
                )
            }
        }
    }
}