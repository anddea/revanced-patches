package app.revanced.patches.music.layout.branding.name.patch

import app.revanced.extensions.startsWithAny
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import org.w3c.dom.Element

@Patch(false)
@Name("custom-branding-music-name")
@Description("Changes the Music launcher name to your choice (defaults to YTM Extended, ReVanced Music Extended).")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CustomBrandingMusicNamePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        // App name
        val resourceFileNames = arrayOf("strings.xml")
        val longName = MusicLongName
        val shortName = MusicShortName

        context.forEach {
            if (!it.name.startsWithAny(*resourceFileNames)) return@forEach

            context.xmlEditor[it.absolutePath].use { editor ->
            val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i)
                    if (node !is Element) continue

                    val element = resourcesNode.childNodes.item(i) as Element
                    element.textContent = when (element.getAttribute("name")) {
                        "app_name" -> "$longName"
                        "app_launcher_name" -> "$shortName"
                        else -> continue
                    }
                }
            }
        }

        return PatchResultSuccess()
    }
    companion object : OptionsContainer() {
        var MusicLongName: String? by option(
            PatchOption.StringOption(
                key = "MusicLongName",
                default = "ReVanced Music Extended",
                title = "Application Name of YouTube Music",
                description = "The name of the YouTube Music it will show on your notification panel."
            )
        )
        var MusicShortName: String? by option(
            PatchOption.StringOption(
                key = "MusicShortName",
                default = "YTM Extended",
                title = "Application Name of YouTube Music",
                description = "The name of the YouTube Music it will show on your home screen."
            )
        )
    }
}
