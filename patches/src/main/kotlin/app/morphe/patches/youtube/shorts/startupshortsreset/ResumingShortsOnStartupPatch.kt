package app.morphe.patches.youtube.shorts.startupshortsreset

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.SHORTS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_RESUMING_SHORTS_ON_STARTUP
import app.morphe.patches.youtube.utils.playservice.is_20_02_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
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
