package app.revanced.patches.music.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.resources.IconHelper.YOUTUBE_MUSIC_LAUNCHER_ICON_ARRAY
import app.revanced.util.resources.IconHelper.copyFiles
import app.revanced.util.resources.IconHelper.makeDirectoryAndCopyFiles
import app.revanced.util.resources.ResourceUtils

@Patch(false)
@Name("custom-branding-music-manual")
@Description("Changes the YouTube Music launcher icon specified in the 'branding-music' directory.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CustomBrandingMusicIconManualPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        fun copyResources(resourceGroups: List<ResourceUtils.ResourceGroup>) {
            val iconPath = "branding-music"
            try {
                context.copyFiles(resourceGroups, iconPath)
            } catch (_: Exception) {
                context.makeDirectoryAndCopyFiles(resourceGroups, iconPath)
                throw PatchResultError("icon not found!")
            }
        }

        val iconResourceFileNames =
            YOUTUBE_MUSIC_LAUNCHER_ICON_ARRAY
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

        return PatchResultSuccess()
    }
}
