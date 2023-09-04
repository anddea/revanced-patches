package app.revanced.patches.youtube.navigation.tabletnavbar.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.navigation.tabletnavbar.fingerprints.PivotBarChangedFingerprint
import app.revanced.patches.youtube.navigation.tabletnavbar.fingerprints.PivotBarStyleFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Enable tablet navigation bar")
@Description("Enables the tablet navigation bar.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class TabletNavigationBarPatch : BytecodePatch(
    listOf(
        PivotBarChangedFingerprint,
        PivotBarStyleFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        arrayOf(
            PivotBarChangedFingerprint,
            PivotBarStyleFingerprint
        ).forEach {
            it.result?.insertHook() ?: throw it.exception
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: ENABLE_TABLET_NAVIGATION_BAR"
            )
        )

        SettingsPatch.updatePatchStatus("enable-tablet-navigation-bar")

    }

    companion object {
        private fun MethodFingerprintResult.insertHook() {
            val targetIndex = scanResult.patternScanResult!!.startIndex + 1
            val register =
                mutableMethod.getInstruction<OneRegisterInstruction>(targetIndex).registerA

            mutableMethod.addInstructions(
                targetIndex + 1, """
                    invoke-static {v$register}, $NAVIGATION->enableTabletNavBar(Z)Z
                    move-result v$register
                    """
            )
        }
    }
}
