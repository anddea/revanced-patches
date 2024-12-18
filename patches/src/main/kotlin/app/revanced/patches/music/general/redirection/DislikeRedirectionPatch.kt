package app.revanced.patches.music.general.redirection

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.patch.PatchList.DISABLE_DISLIKE_REDIRECTION
import app.revanced.patches.music.utils.pendingIntentReceiverFingerprint
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Suppress("unused")
val dislikeRedirectionPatch = bytecodePatch(
    DISABLE_DISLIKE_REDIRECTION.title,
    DISABLE_DISLIKE_REDIRECTION.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        lateinit var onClickReference: Reference

        pendingIntentReceiverFingerprint.methodOrThrow().apply {
            val startIndex = indexOfFirstStringInstructionOrThrow("YTM Dislike")
            val onClickRelayIndex =
                indexOfFirstInstructionReversedOrThrow(startIndex, Opcode.INVOKE_VIRTUAL)
            val onClickRelayMethod = getWalkerMethod(onClickRelayIndex)

            onClickRelayMethod.apply {
                val onClickMethodIndex =
                    indexOfFirstInstructionReversedOrThrow(Opcode.INVOKE_DIRECT)
                val onClickMethod = getWalkerMethod(onClickMethodIndex)

                onClickMethod.apply {
                    val onClickIndex = indexOfFirstInstructionOrThrow {
                        val reference =
                            ((this as? ReferenceInstruction)?.reference as? MethodReference)

                        opcode == Opcode.INVOKE_INTERFACE &&
                                reference?.returnType == "V" &&
                                reference.parameterTypes.size == 1
                    }
                    onClickReference =
                        getInstruction<ReferenceInstruction>(onClickIndex).reference

                    disableDislikeRedirection(onClickIndex)
                }
            }
        }

        dislikeButtonOnClickListenerFingerprint.methodOrThrow().apply {
            val onClickIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.toString() == onClickReference.toString()
            }
            disableDislikeRedirection(onClickIndex)
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_dislike_redirection",
            "false"
        )

        updatePatchStatus(DISABLE_DISLIKE_REDIRECTION)

    }
}

private fun MutableMethod.disableDislikeRedirection(onClickIndex: Int) {
    val targetIndex = indexOfFirstInstructionReversedOrThrow(onClickIndex, Opcode.IF_EQZ)
    val insertRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

    addInstructionsWithLabels(
        targetIndex + 1, """
            invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->disableDislikeRedirection()Z
            move-result v$insertRegister
            if-nez v$insertRegister, :disable
            """, ExternalLabel("disable", getInstruction(onClickIndex + 1))
    )
}
