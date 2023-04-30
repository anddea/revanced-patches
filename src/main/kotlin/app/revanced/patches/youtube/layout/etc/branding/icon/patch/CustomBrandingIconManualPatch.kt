package app.revanced.patches.youtube.layout.etc.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.IconHelper.YOUTUBE_LAUNCHER_ICON_ARRAY
import app.revanced.util.resources.IconHelper.copyFiles
import app.revanced.util.resources.IconHelper.makeDirectoryAndCopyFiles
import app.revanced.util.resources.ResourceHelper.updatePatchStatusIcon
import app.revanced.util.resources.ResourceUtils


@Patch(false)
@Name("custom-branding-icon-manual")
@Description("Changes the YouTube launcher icon specified in the 'branding' directory.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class CustomBrandingIconManualPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        fun copyResources(resourceGroups: List<ResourceUtils.ResourceGroup>) {
            val iconPath = "branding"

            try {
                context.copyFiles(resourceGroups, iconPath)
            } catch (_: Exception) {
                context.makeDirectoryAndCopyFiles(resourceGroups, iconPath)
                throw PatchResultError("icon not found!")
            }
        }

        val iconResourceFileNames =
            YOUTUBE_LAUNCHER_ICON_ARRAY
                .map { "$it.png" }
                .toTypedArray()

        fun createGroup(directory: String) = ResourceUtils.ResourceGroup(
            directory, *iconResourceFileNames
        )

        // change the app icon
        arrayOf("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi")
            .map { "mipmap-$it" }
            .map(::createGroup)
            .let(::copyResources)

        context.updatePatchStatusIcon("custom")

        return PatchResultSuccess()
    }
}
