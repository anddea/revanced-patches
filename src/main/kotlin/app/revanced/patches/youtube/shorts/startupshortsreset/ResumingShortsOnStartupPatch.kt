package app.revanced.patches.youtube.shorts.startupshortsreset

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints.UserWasInShortsABConfigFingerprint
import app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints.UserWasInShortsFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getReference
import app.revanced.util.getTargetIndex
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object ResumingShortsOnStartupPatch : BaseBytecodePatch(
    name = "Disable resuming shorts on startup",
    description = "Adds an option to disable the Shorts player from resuming on app startup when Shorts were last being watched.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        UserWasInShortsABConfigFingerprint,
        UserWasInShortsFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        UserWasInShortsABConfigFingerprint.resultOrThrow().let {
            val walkerMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex)

            // This method will only be called for the user being A/B tested.
            // Presumably a method that processes the ProtoDataStore value (boolean) for the 'user_was_in_shorts' key.
            walkerMethod.apply {
                val insertIndex = getTargetIndex(Opcode.IGET_OBJECT)
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->disableResumingStartupShortsPlayer()Z
                        move-result v$insertRegister
                        if-eqz v$insertRegister, :show
                        const/4 v$insertRegister, 0x0
                        return v$insertRegister
                        """, ExternalLabel("show", getInstruction(insertIndex))
                )
            }
        }

        UserWasInShortsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val listenableInstructionIndex = indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_INTERFACE &&
                            getReference<MethodReference>()?.definingClass == "Lcom/google/common/util/concurrent/ListenableFuture;" &&
                            getReference<MethodReference>()?.name == "isDone"
                }
                if (listenableInstructionIndex < 0) throw PatchException("Could not find instruction index")
                val originalInstructionRegister = getInstruction<FiveRegisterInstruction>(listenableInstructionIndex).registerC
                val freeRegister = getInstruction<OneRegisterInstruction>(listenableInstructionIndex + 1).registerA

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

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SHORTS",
                "SETTINGS: DISABLE_RESUMING_SHORTS_PLAYER"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
