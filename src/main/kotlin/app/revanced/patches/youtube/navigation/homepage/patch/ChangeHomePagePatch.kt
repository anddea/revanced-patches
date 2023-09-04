package app.revanced.patches.youtube.navigation.homepage.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.navigation.homepage.fingerprints.IntentExceptionFingerprint
import app.revanced.patches.youtube.navigation.homepage.fingerprints.LauncherActivityFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION

@Patch
@Name("Change homepage")
@Description("Change home page to subscription feed.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class ChangeHomePagePatch : BytecodePatch(
    listOf(
        IntentExceptionFingerprint,
        LauncherActivityFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        LauncherActivityFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    implementation!!.instructions.size - 1, """
                        move-object/from16 v0, p1
                        invoke-static {v0}, $NAVIGATION->changeHomePage(Landroid/app/Activity;)V
                        """
                )
            }
        } ?: throw LauncherActivityFingerprint.exception

        IntentExceptionFingerprint.result?.let {
            it.mutableMethod.apply {
                val index = it.scanResult.patternScanResult!!.endIndex + 1

                addInstructionsWithLabels(
                    index, """
                        invoke-static {}, $NAVIGATION->changeHomePage()Z
                        move-result v0
                        if-eqz v0, :default
                        return-void
                        """, ExternalLabel("default", getInstruction(index))
                )
            }
        } ?: throw IntentExceptionFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: CHANGE_HOMEPAGE_TO_SUBSCRIPTION"
            )
        )

        SettingsPatch.updatePatchStatus("change-homepage")

    }
}