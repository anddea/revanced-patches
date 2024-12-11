package app.revanced.patches.youtube.misc.backgroundplayback

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getWalkerMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val backgroundPlaybackPatch = bytecodePatch(
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.title,
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        playerTypeHookPatch,
        settingsPatch,
    )

    execute {

        backgroundPlaybackManagerFingerprint.methodOrThrow().apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                // Replace to preserve control flow label.
                replaceInstruction(
                    index,
                    "invoke-static { v$register }, $MISC_PATH/BackgroundPlaybackPatch;->allowBackgroundPlayback(Z)Z"
                )

                addInstructions(
                    index + 1,
                    """
                        move-result v$register
                        return v$register
                        """
                )
            }
        }

        // Enable background playback option in YouTube settings
        backgroundPlaybackSettingsFingerprint.methodOrThrow().apply {
            val booleanCalls = implementation!!.instructions.withIndex()
                .filter { instruction ->
                    ((instruction.value as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "Z"
                }

            val booleanIndex = booleanCalls.elementAt(1).index
            val booleanMethod = getWalkerMethod(booleanIndex)

            booleanMethod.addInstructions(
                0, """
                    const/4 v0, 0x1
                    return v0
                    """
            )
        }

        // Force allowing background play for videos labeled for kids.
        kidsBackgroundPlaybackPolicyControllerFingerprint.methodOrThrow(
            kidsBackgroundPlaybackPolicyControllerParentFingerprint
        ).addInstruction(
            0,
            "return-void"
        )

        pipControllerFingerprint.matchOrThrow().let {
            val targetMethod =
                it.getWalkerMethod(it.patternMatch!!.endIndex)

            targetMethod.apply {
                val targetRegister = getInstruction<TwoRegisterInstruction>(0).registerA

                addInstruction(
                    1,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        // region add settings

        addPreference(REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS)

        // endregion

    }
}
