package app.revanced.patches.youtube.shorts.startupshortsreset

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.DISABLE_RESUMING_SHORTS_ON_STARTUP
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
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

    dependsOn(settingsPatch)

    execute {

        userWasInShortsABConfigFingerprint.methodOrThrow().apply {
            val startIndex = indexOfOptionalInstruction(this)
            val walkerIndex = implementation!!.instructions.let {
                val subListIndex =
                    it.subList(startIndex, startIndex + 20).indexOfFirst { instruction ->
                        val reference = instruction.getReference<MethodReference>()
                        instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                                reference?.returnType == "Z" &&
                                reference.definingClass != "Lj${'$'}/util/Optional;" &&
                                reference.parameterTypes.isEmpty()
                    }
                if (subListIndex < 0)
                    throw PatchException("subListIndex not found")

                startIndex + subListIndex
            }
            val walkerMethod = getWalkerMethod(walkerIndex)

            // This method will only be called for the user being A/B tested.
            // Presumably a method that processes the ProtoDataStore value (boolean) for the 'user_was_in_shorts' key.
            walkerMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->disableResumingStartupShortsPlayer()Z
                        move-result v0
                        if-eqz v0, :show
                        const/4 v0, 0x0
                        return v0
                        """, ExternalLabel("show", getInstruction(0))
                )
            }
        }

        userWasInShortsFingerprint.methodOrThrow().apply {
            val listenableInstructionIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_INTERFACE &&
                        getReference<MethodReference>()?.definingClass == "Lcom/google/common/util/concurrent/ListenableFuture;" &&
                        getReference<MethodReference>()?.name == "isDone"
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
