package app.revanced.patches.youtube.layout.navigation.subscriptionsbutton.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.lastpivottab.patch.LastPivotTabHookPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch

@Patch
@Name("hide-subscriptions-button")
@Description("Hides the subscriptions button in the navigation bar.")
@DependsOn(
    [
        SettingsPatch::class,
        LastPivotTabHookPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SubscriptionsButtonRemoverPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_SUBSCRIPTIONS_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-subscriptions-button")

        return PatchResultSuccess()
    }
}