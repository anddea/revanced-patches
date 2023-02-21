package app.revanced.patches.youtube.layout.etc.materialyou.patch

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
import app.revanced.patches.youtube.layout.etc.theme.bytecode.patch.GeneralThemeBytecodePatch.Companion.isMonetPatchIncluded
import app.revanced.patches.youtube.layout.etc.theme.resource.patch.GeneralThemePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper.updatePatchStatusTheme
import app.revanced.util.resources.ResourceUtils.copyXmlNode
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Patch(false)
@Name("materialyou")
@Description("Enables MaterialYou theme for Android 12+")
@DependsOn(
    [
        GeneralThemePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class MaterialYouPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        val drawables1 = "drawable-night-v31" to arrayOf(
            "new_content_dot_background.xml"
        )

        val drawables2 = "drawable-v31" to arrayOf(
            "new_content_count_background.xml",
            "new_content_dot_background.xml"
        )

        val layout1 = "layout-v31" to arrayOf(
            "new_content_count.xml"
        )

        arrayOf(drawables1, drawables2, layout1).forEach { (path, resourceNames) ->
            Files.createDirectory(context["res"].resolve(path).toPath())
            resourceNames.forEach { name ->
                val monetPath = "$path/$name"

                Files.copy(
                    this.javaClass.classLoader.getResourceAsStream("youtube/materialyou/$monetPath")!!,
                    context["res"].resolve(monetPath).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        context.copyXmlNode("youtube/materialyou/host", "values-v31/colors.xml", "resources")

        /*
         * Add settings
         */
        context.updatePatchStatusTheme("materialyou")

        isMonetPatchIncluded = true

        return PatchResultSuccess()
    }
}
