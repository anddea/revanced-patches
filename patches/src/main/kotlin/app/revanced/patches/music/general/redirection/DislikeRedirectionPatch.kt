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
import app.revanced.patches.music.utils.playservice.is_7_29_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
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

var onClickReference = ""

@Suppress("unused")
val dislikeRedirectionPatch = bytecodePatch(
    DISABLE_DISLIKE_REDIRECTION.title,
    DISABLE_DISLIKE_REDIRECTION.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    execute {
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
                    val relativeIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()
                                    ?.parameterTypes
                                    ?.contains("Ljava/util/Map;") == true
                    }
                    val onClickIndex = indexOfFirstInstructionOrThrow(relativeIndex) {
                        val reference = getReference<MethodReference>()

                        opcode == Opcode.INVOKE_INTERFACE &&
                                reference?.returnType == "V" &&
                                reference.parameterTypes.size == 1
                    }
                    onClickReference =
                        getInstruction<ReferenceInstruction>(onClickIndex).reference.toString()

                    disableDislikeRedirection(onClickIndex)
                }
            }
        }

        if (is_7_29_or_greater) {
            dislikeButtonOnClickListenerAlternativeFingerprint
                .methodOrThrow()
                .disableDislikeRedirection()
        } else {
            dislikeButtonOnClickListenerFingerprint
                .methodOrThrow()
                .disableDislikeRedirection()
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_dislike_redirection",
            "false"
        )

        updatePatchStatus(DISABLE_DISLIKE_REDIRECTION)

    }
}

private fun MutableMethod.disableDislikeRedirection(startIndex: Int = 0) {
    val onClickIndex =
        if (startIndex == 0) {
            indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.toString() == onClickReference
            }
        } else {
            startIndex
        }
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
