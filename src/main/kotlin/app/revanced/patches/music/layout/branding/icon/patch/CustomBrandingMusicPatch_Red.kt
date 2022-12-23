package app.revanced.patches.music.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Patch
@Name("custom-branding-music-red")
@Description("Changes the YouTube Music launcher icon to your choice (defaults to ReVanced Red).")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CustomBrandingMusicPatch_Red : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        val classLoader = this.javaClass.classLoader
        val resDirectory = context["res"]
        if (!resDirectory.isDirectory) return PatchResultError("The res folder can not be found.")

        // App Icon
        val AppiconNames = arrayOf(
            "adaptiveproduct_youtube_music_background_color_108",
            "adaptiveproduct_youtube_music_foreground_color_108",
            "ic_launcher_release"
        )

        mapOf(
            "xxxhdpi" to 192,
            "xxhdpi" to 144,
            "xhdpi" to 96,
            "hdpi" to 72,
            "mdpi" to 48
        ).forEach { (iconDirectory, size) ->
            AppiconNames.forEach iconLoop@{ iconName ->
                Files.copy(
                    classLoader.getResourceAsStream("music/branding/red/launchericon/$size/$iconName.png")!!,
                    resDirectory.resolve("mipmap-$iconDirectory").resolve("$iconName.png").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        // Other Resource
        val drawables1 = "drawable-hdpi" to arrayOf(
            "action_bar_logo",
            "action_bar_logo_release",
            "record"
        )

        val drawables2 = "drawable-large-hdpi" to arrayOf(
            "record"
        )

        val drawables3 = "drawable-large-mdpi" to arrayOf(
            "record"
        )

        val drawables4 = "drawable-large-xhdpi" to arrayOf(
            "record"
        )

        val drawables5 = "drawable-mdpi" to arrayOf(
            "action_bar_logo",
            "record"
        )

        val drawables6 = "drawable-xhdpi" to arrayOf(
            "action_bar_logo",
            "record"
        )

        val drawables7 = "drawable-xlarge-hdpi" to arrayOf(
            "record"
        )

        val drawables8 = "drawable-xlarge-mdpi" to arrayOf(
            "record"
        )

        val drawables9 = "drawable-xxhdpi" to arrayOf(
            "action_bar_logo",
            "record"
        )

        val drawables10 = "drawable-xxxhdpi" to arrayOf(
            "action_bar_logo"
        )

        val pngResources = arrayOf(drawables1, drawables2, drawables3, drawables4, drawables5, drawables6, drawables7, drawables8, drawables9, drawables10)

        pngResources.forEach { (path, resourceNames) ->
            resourceNames.forEach { name ->
                val relativePath = "$path/$name.png"

                Files.copy(
                    classLoader.getResourceAsStream("music/branding/red/resource/$relativePath")!!,
                    context["res"].resolve(relativePath).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        return PatchResultSuccess()
    }

}
