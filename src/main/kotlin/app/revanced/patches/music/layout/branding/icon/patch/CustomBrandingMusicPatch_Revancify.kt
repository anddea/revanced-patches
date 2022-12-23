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

@Patch(false)
@Name("custom-branding-music-revancify")
@Description("Changes the YouTube Music launcher icon to your choice (Revancify).")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CustomBrandingMusicPatch_Revancify : ResourcePatch {
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
                    classLoader.getResourceAsStream("music/branding/revancify/launchericon/$size/$iconName.png")!!,
                    resDirectory.resolve("mipmap-$iconDirectory").resolve("$iconName.png").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        // MonoChrome Icon
        arrayOf("drawable" to arrayOf("ic_app_icons_themed_youtube_music")).forEach { (path, resourceNames) ->
            resourceNames.forEach { name ->
                val relativePath = "$path/$name.xml"

                Files.copy(
                    classLoader.getResourceAsStream("music/branding/revancify/monochromeicon/$relativePath")!!,
                    context["res"].resolve(relativePath).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        return PatchResultSuccess()
    }

}
