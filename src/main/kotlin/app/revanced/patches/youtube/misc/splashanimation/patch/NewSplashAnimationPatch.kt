package app.revanced.patches.youtube.misc.splashanimation.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.misc.splashanimation.fingerprints.SplashAnimationBuilderFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWide32LiteralIndex
import app.revanced.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Enable new splash animation")
@Description("Enables a new type of splash animation on Android 12+ devices.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class NewSplashAnimationPatch : BytecodePatch(
    listOf(SplashAnimationBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SplashAnimationBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                var targetIndex = getWide32LiteralIndex(45407550) + 3
                if (getInstruction(targetIndex).opcode == Opcode.MOVE_RESULT)
                    targetIndex += 1

                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {}, $MISC_PATH/SplashAnimationPatch;->enableNewSplashAnimation()Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: return SplashAnimationBuilderFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_NEW_SPLASH_ANIMATION"
            )
        )

        SettingsPatch.updatePatchStatus("enable-new-splash-animation")

        return PatchResultSuccess()
    }
}
