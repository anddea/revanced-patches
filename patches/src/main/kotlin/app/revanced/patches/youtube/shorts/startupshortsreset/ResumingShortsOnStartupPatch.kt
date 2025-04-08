package app.revanced.patches.youtube.shorts.startupshortsreset

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.DISABLE_RESUMING_SHORTS_ON_STARTUP
import app.revanced.patches.youtube.utils.playservice.is_20_02_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val resumingShortsOnStartupPatch = bytecodePatch(
    DISABLE_RESUMING_SHORTS_ON_STARTUP.title,
    DISABLE_RESUMING_SHORTS_ON_STARTUP.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        userWasInShortsConfigFingerprint
            .methodOrThrow()
            .addInstructionsWithLabels(
                0, """
                    invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->disableResumingStartupShortsPlayer()Z
                    move-result v0
                    if-eqz v0, :show
                    const/4 v0, 0x0
                    return v0
                    :show
                    nop
                    """
            )

        if (is_20_02_or_greater) {
            userWasInShortsAlternativeFingerprint.matchOrThrow().let {
                it.method.apply {
                    val stringIndex = it.stringMatches!!.first().index
                    val booleanValueIndex = indexOfFirstInstructionReversedOrThrow(stringIndex) {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.name == "booleanValue"
                    }
                    val booleanValueRegister =
                        getInstruction<OneRegisterInstruction>(booleanValueIndex + 1).registerA

                    addInstructions(
                        booleanValueIndex + 2, """
                            invoke-static {v$booleanValueRegister}, $SHORTS_CLASS_DESCRIPTOR->disableResumingStartupShortsPlayer(Z)Z
                            move-result v$booleanValueRegister
                            """
                    )
                }
            }
        } else {
            userWasInShortsFingerprint.methodOrThrow().apply {
                val listenableInstructionIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_INTERFACE &&
                            reference?.definingClass == "Lcom/google/common/util/concurrent/ListenableFuture;" &&
                            reference.name == "isDone"
                }
                val originalInstructionRegister =
                    getInstruction<FiveRegisterInstruction>(listenableInstructionIndex).registerC
                val freeRegister =
                    getInstruction<OneRegisterInstruction>(listenableInstructionIndex + 1).registerA

                addInstructionsWithLabels(
                    listenableInstructionIndex + 1,
                    """
                        invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->disableResumingStartupShortsPlayer()Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :show
                        return-void
                        :show
                        invoke-interface {v$originalInstructionRegister}, Lcom/google/common/util/concurrent/ListenableFuture;->isDone()Z
                        """
                )
                removeInstruction(listenableInstructionIndex)
            }

        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SHORTS",
                "SETTINGS: DISABLE_RESUMING_SHORTS_PLAYER"
            ),
            DISABLE_RESUMING_SHORTS_ON_STARTUP
        )

        // endregion

    }
}
