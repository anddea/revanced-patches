package app.revanced.patches.shared.ads

import app.revanced.patcher.Match
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/FullscreenAdsPatch;"

fun baseAdsPatch(
    classDescriptor: String,
    methodDescriptor: String,
) = bytecodePatch(
    description = "baseAdsPatch"
) {
    execute {

        videoAdsFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $classDescriptor->$methodDescriptor()Z
                    move-result v0
                    if-nez v0, :show_ads
                    return-void
                    """, ExternalLabel("show_ads", getInstruction(0))
            )
        }

        musicAdsFingerprint.methodOrThrow().apply {
            val targetIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_VIRTUAL &&
                        reference?.returnType == "V" &&
                        reference.parameterTypes.size == 1 &&
                        reference.parameterTypes.first() == "Z"
            }

            getWalkerMethod(targetIndex)
                .addInstructions(
                    0, """
                        invoke-static {p1}, $classDescriptor->$methodDescriptor(Z)Z
                        move-result p1
                        """
                )
        }

    }
}

internal fun MutableMethod.hookNonLithoFullscreenAds(literal: Long) {
    val targetIndex = indexOfFirstLiteralInstructionOrThrow(literal) + 2
    val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

    addInstruction(
        targetIndex + 1,
        "invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideFullscreenAds(Landroid/view/View;)V"
    )
}

internal fun Match.hookLithoFullscreenAds() {
    method.apply {
        val dialogCodeIndex = patternMatch!!.endIndex
        val dialogCodeField =
            getInstruction<ReferenceInstruction>(dialogCodeIndex).reference as FieldReference
        if (dialogCodeField.type != "I")
            throw PatchException("Invalid dialogCodeField: $dialogCodeField")

        var prependInstructions = """
            move-object/from16 v0, p1
            move-object/from16 v1, p2
            """

        if (parameterTypes.firstOrNull() != "[B") {
            val toByteArrayReference = getInstruction<ReferenceInstruction>(
                indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "toByteArray"
                }
            ).reference

            prependInstructions += """
                invoke-virtual {v0}, $toByteArrayReference
                move-result-object v0
                """
        }

        // Disable fullscreen ads
        addInstructionsWithLabels(
            0, prependInstructions + """
                check-cast v1, ${dialogCodeField.definingClass}
                iget v1, v1, $dialogCodeField
                invoke-static {v0, v1}, $EXTENSION_CLASS_DESCRIPTOR->disableFullscreenAds([BI)Z
                move-result v1
                if-eqz v1, :show
                return-void
                """, ExternalLabel("show", getInstruction(0))
        )
    }
}

