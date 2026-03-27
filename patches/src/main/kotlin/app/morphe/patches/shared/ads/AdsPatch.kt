package app.morphe.patches.shared.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.mapping.ResourceType.ID
import app.morphe.patches.shared.mapping.ResourceType.STYLE
import app.morphe.patches.shared.mapping.getResourceId
import app.morphe.patches.shared.mapping.resourceMappingPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

var interstitialsContainer = -1L
    private set
var slidingDialogAnimation = -1L
    private set

private val adsResourcePatch = resourcePatch(
    description = "adsResourcePatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        interstitialsContainer = getResourceId(ID, "interstitials_container")
        slidingDialogAnimation = getResourceId(STYLE, "SlidingDialogAnimation")
    }
}

fun adsPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    classDescriptor: String,
    methodDescriptor: String,
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    name = "Hide ads",
    description = "Adds options to hide ads."
) {
    block()

    dependsOn(adsResourcePatch)

    execute {
        videoAdsLegacyFingerprint.methodOrThrow().apply {
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

        arrayOf(
            playerBytesAdLayoutFingerprint,
            videoAdsFingerprint,
        ).forEach { fingerprint ->
            fingerprint.methodOrThrow().addInstructionsWithLabels(
                0, """
                    invoke-static {}, $classDescriptor->$methodDescriptor()Z
                    move-result v0
                    if-eqz v0, :ignore
                    return-void
                    :ignore
                    nop
                    """
            )
        }

        // non-litho view, used in some old clients
        interstitialsContainerFingerprint.methodOrThrow().apply {
            val targetIndex = indexOfFirstLiteralInstructionOrThrow(interstitialsContainer) + 2
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideFullscreenAds(Landroid/view/View;)V"
            )
        }

        // litho view, used in 'ShowDialogCommandOuterClass' in innertube
        showDialogCommandFingerprint.methodOrThrow().apply {
            // It is ideal to check the dialog type and proto buffer before closing the dialog.
            // There is no register that can be used freely, so it is divided into two hooking.
            val showDialogIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "show"
            }
            val dialogReference =
                getInstruction<ReferenceInstruction>(showDialogIndex).reference
            val dialogClass = (dialogReference as MethodReference).definingClass

            val setDialogIndex = indexOfFirstInstructionReversedOrThrow {
                opcode == Opcode.IPUT_OBJECT &&
                        getReference<FieldReference>()?.type == dialogClass
            }
            val dialogRegister =
                getInstruction<TwoRegisterInstruction>(setDialogIndex).registerA

            addInstructionsAtControlFlowLabel(
                setDialogIndex,
                "invoke-static {v$dialogRegister}, $EXTENSION_CLASS_DESCRIPTOR->dismissDialog(Ljava/lang/Object;)V"
            )

            var prependInstructions = """
                move-object/from16 v0, p1
                """

            // Used only in very old versions.
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
            addInstructions(
                0, prependInstructions + """
                    invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->checkDialog([B)V
                    """
            )

            // Set close dialog method
            val customDialogOnBackPressedMethod = customDialogOnBackPressedFingerprint
                .methodOrThrow(customDialogOnBackPressedParentFingerprint)

            fullscreenAdsPatchFingerprint.methodOrThrow().addInstructions(
                0, """
                    check-cast p0, ${customDialogOnBackPressedMethod.definingClass}
                    invoke-virtual { p0 }, $customDialogOnBackPressedMethod
                    """
            )
        }

        executeBlock()
    }
}
