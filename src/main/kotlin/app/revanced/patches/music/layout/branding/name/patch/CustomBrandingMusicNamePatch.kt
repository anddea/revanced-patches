package app.revanced.patches.music.layout.branding.name.patch

import app.revanced.extensions.startsWithAny
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.options.PatchOptions
import org.w3c.dom.Element

@Patch(false)
@Name("custom-branding-music-name")
@DependsOn([PatchOptions::class])
@Description("Changes the Music launcher name to your choice (defaults to RVX Music, ReVanced Extended Music).")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CustomBrandingMusicNamePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        // App name
        val resourceFileNames = arrayOf("strings.xml")
        val fullName = PatchOptions.MusicAppNameFull
        val shortName = PatchOptions.MusicAppNameShort

        context.forEach {
            if (!it.name.startsWithAny(*resourceFileNames)) return@forEach

            context.xmlEditor[it.absolutePath].use { editor ->
            val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i)
                    if (node !is Element) continue

                    val element = resourcesNode.childNodes.item(i) as Element
                    element.textContent = when (element.getAttribute("name")) {
                        "app_name" -> "$fullName"
                        "app_launcher_name" -> "$shortName"
                        else -> continue
                    }
                }
            }
        }

        return PatchResultSuccess()
    }
}
