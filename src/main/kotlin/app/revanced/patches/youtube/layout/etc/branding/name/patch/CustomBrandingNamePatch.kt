package app.revanced.patches.youtube.layout.etc.branding.name.patch

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
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.options.PatchOptions
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper.updatePatchStatusLabel
import org.w3c.dom.Element

@Patch
@Name("custom-branding-name")
@DependsOn(
    [
        PatchOptions::class,
        SettingsPatch::class
    ]
)
@Description("Changes the YouTube launcher name to your choice (defaults to ReVanced Extended).")
@YouTubeCompatibility
@Version("0.0.1")
class CustomBrandingNamePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        // App name
        val resourceFileNames = arrayOf("strings.xml")
        val appName = PatchOptions.YouTubeAppName

        context.forEach {
            if (!it.name.startsWithAny(*resourceFileNames)) return@forEach

            // for each file in the "layouts" directory replace all necessary attributes content
            context.xmlEditor[it.absolutePath].use { editor ->
            val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i)
                    if (node !is Element) continue

                    val element = resourcesNode.childNodes.item(i) as Element
                    element.textContent = when (element.getAttribute("name")) {
                        "application_name" -> "$appName"
                        else -> continue
                    }
                }
            }
        }

        context.updatePatchStatusLabel("$appName")

        return PatchResultSuccess()
    }
}
