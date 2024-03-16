package app.revanced.patches.youtube.shorts.startupshortsreset

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints.UserWasInShortsFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Disable shorts on startup",
    description = "Adds an option to disable the Shorts player from resuming on app startup when Shorts were last being watched.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40"
            ]
        )
    ]
)
@Suppress("unused")
object DisableShortsOnStartupPatch : BytecodePatch(
    setOf(UserWasInShortsFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        UserWasInShortsFingerprint.result?.let {
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
                        invoke-static { }, $SHORTS->disableStartupShortsPlayer()Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :show_startup_shorts_player
                        return-void
                        :show_startup_shorts_player
                        invoke-interface {v$originalInstructionRegister}, Lcom/google/common/util/concurrent/ListenableFuture;->isDone()Z
                        """
                )
                // Remove original instruction to preserve control flow label.
                removeInstruction(listenableInstructionIndex)
            }
        } ?: throw UserWasInShortsFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SHORTS_SETTINGS",
                "SETTINGS: SHORTS_PLAYER_PARENT",
                "SETTINGS: DISABLE_STARTUP_SHORTS_PLAYER"
            )
        )

        SettingsPatch.updatePatchStatus("Disable shorts on startup")

    }
}
