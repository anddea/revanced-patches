package app.revanced.patches.shared.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatchBuilder
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.Option
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.patches.shared.mapping.ResourceType.ID
import app.revanced.patches.shared.mapping.ResourceType.STYLE
import app.revanced.patches.shared.mapping.getResourceId
import app.revanced.patches.shared.mapping.resourceMappingPatch
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
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

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/FullscreenAdsPatch;"

fun adsPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    classDescriptor: String,
    methodDescriptor: String,
    hideFullscreenAdsOption: BytecodePatchBuilder.() -> Option<Boolean>? = { null },
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

        val hideFullscreenAdsOption = hideFullscreenAdsOption()
        val hideFullscreenAds =
            hideFullscreenAdsOption == null || hideFullscreenAdsOption.value == true

        if (!hideFullscreenAds) {
            executeBlock()
            return@execute
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
        showDialogCommandFingerprint.matchOrThrow().let {
            it.method.apply {
                // It is ideal to check the dialog type and protobuffer before closing the dialog.
                // There is no register that can be used freely, so it is divided into two hooking.
                val showDialogIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "show"
                }
                val dialogRegister =
                    getInstruction<FiveRegisterInstruction>(showDialogIndex).registerC

                addInstruction(
                    showDialogIndex + 1,
                    "invoke-static {v$dialogRegister}, $EXTENSION_CLASS_DESCRIPTOR->dismissDialog(Ljava/lang/Object;)V"
                )

                // Dialog type should be checked first.
                val dialogCodeIndex = it.patternMatch!!.endIndex
                val dialogCodeField =
                    getInstruction<ReferenceInstruction>(dialogCodeIndex).reference as FieldReference
                if (dialogCodeField.type != "I") {
                    throw PatchException("Invalid dialogCodeField: $dialogCodeField")
                }

                var prependInstructions = """
                    move-object/from16 v0, p1
                    move-object/from16 v1, p2
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
                        check-cast v1, ${dialogCodeField.definingClass}
                        iget v1, v1, $dialogCodeField
                        invoke-static {v0, v1}, $EXTENSION_CLASS_DESCRIPTOR->checkDialog([BI)V
                        """
                )
            }
        }

        executeBlock()
    }
}
