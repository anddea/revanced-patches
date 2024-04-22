package app.revanced.patches.youtube.general.rollingnumber

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.fingerprints.RollingNumberTextViewAnimationUpdateFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Disable rolling number animations",
    description = "Adds an option to disable rolling number animations of video view count, user likes, and upload time.",
    dependencies = [SettingsPatch::class],
compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
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
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.43",
                "19.12.41",
                "19.13.37",
                "19.14.43"
            ]
        )
    ]
)
@Suppress("unused")
object DisableRollingNumberAnimationPatch : BytecodePatch(
    setOf(
        RollingNumberTextViewAnimationUpdateFingerprint
    )
) {

    override fun execute(context: BytecodeContext) {

        // Animations are disabled by preventing an Image from being applied to the text span,
        // which prevents the animations from appearing.
        if (SettingsPatch.upward1843) {
            RollingNumberTextViewAnimationUpdateFingerprint.result?.apply {
                val patternScanResult = scanResult.patternScanResult!!
                val blockStartIndex = patternScanResult.startIndex
                val blockEndIndex = patternScanResult.endIndex + 1
                mutableMethod.apply {
                    val freeRegister = getInstruction<OneRegisterInstruction>(blockStartIndex).registerA

                    // ReturnYouTubeDislike also makes changes to this same method,
                    // and must add control flow label to a noop instruction to
                    // ensure RYD patch adds it's changes after the control flow label.
                    addInstructions(blockEndIndex, "nop")

                    addInstructionsWithLabels(
                        blockStartIndex,
                        """
                            invoke-static {}, $GENERAL->disableRollingNumberAnimations()Z
                            move-result v$freeRegister
                            if-nez v$freeRegister, :disable_animations
                        """,
                        ExternalLabel("disable_animations", getInstruction(blockEndIndex))
                    )
                }
            } ?: RollingNumberTextViewAnimationUpdateFingerprint.exception
        } else {
            throw PatchException("This version is not supported. Please use YouTube 18.43.45 or later.")
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: DISABLE_ROLLING_NUMBER_ANIMATIONS"
            )
        )

        SettingsPatch.updatePatchStatus("Disable rolling number animations")

    }
}