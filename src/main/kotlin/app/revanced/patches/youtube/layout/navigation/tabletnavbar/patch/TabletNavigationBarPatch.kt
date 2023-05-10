package app.revanced.patches.youtube.layout.navigation.tabletnavbar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.navigation.tabletnavbar.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-tablet-navigation-bar")
@Description("Enables the tablet navigation bar.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class TabletNavigationBarPatch : BytecodePatch(
    listOf(
        PivotBarChangedFingerprint,
        PivotBarStyleFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            PivotBarChangedFingerprint,
            PivotBarStyleFingerprint
        ).forEach {
            it.result?.insertHook() ?: return it.toErrorResult()
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

        return PatchResultSuccess()
    }
    companion object {
        private fun MethodFingerprintResult.insertHook() {
            val targetIndex = this.scanResult.patternScanResult!!.startIndex + 1
            val register = mutableMethod.instruction<OneRegisterInstruction>(targetIndex).registerA

            mutableMethod.addInstructions(
                targetIndex + 1, """
                    invoke-static {v$register}, $NAVIGATION->enableTabletNavBar(Z)Z
                    move-result v$register
                    """
            )
        }
    }
}
