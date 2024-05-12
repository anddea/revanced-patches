package app.revanced.patches.youtube.general.splashanimation

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.general.splashanimation.fingerprints.SplashAnimationFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.DarkSplashAnimation
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithReferenceReversed
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object SplashAnimationPatch : BaseBytecodePatch(
    name = "Disable splash animation",
    description = "Adds an option to disable splash animation.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(SplashAnimationFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        SplashAnimationFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(DarkSplashAnimation)
                val targetIndex = getTargetIndexWithReferenceReversed(constIndex, "(I)Z") + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex - 1).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->disableSplashAnimation(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_SPLASH_ANIMATION"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
