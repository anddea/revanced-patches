package app.revanced.patches.reddit.layout.navigation.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.navigation.patch.NavigationButtonsPatch.Companion.setValue
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("Hide create button")
@Description("Hide create button at navigation bar.")
@DependsOn(
    [
        NavigationButtonsPatch::class,
        SettingsPatch::class
    ]
)
@RedditCompatibility
@Version("0.0.1")
class CreateButtonPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

        updateSettingsStatus("CreateButtons")

        if (SettingsPatch.RedditSettings == true)
            context.setValue("CreateButtons")

        return PatchResultSuccess()
    }
}
