package app.revanced.patches.youtube.misc.translations.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Patch
@Name("translations")
@Description("Add Crowdin Translations")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class TranslationsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        LANGUAGE_LIST.forEach { language ->
            val directory = "values-" + language + "-v21"
            val relativePath = "$language/strings.xml"

            context["res/$directory"].mkdir()

            Files.copy(
                this.javaClass.classLoader.getResourceAsStream("youtube/translations/$relativePath")!!,
                context["res"].resolve("$directory/strings.xml").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        ResourceHelper.patchSuccess(
            context,
            "translations"
        )

        return PatchResultSuccess()
    }

    private companion object {
        val LANGUAGE_LIST = arrayOf(
            "ar",
            "az-rAZ",
            "bn",
            "be-rBY",
            "de-rDE",
            "el-rGR",
            "es-rES",
            "fr-rFR",
            "hu-rHU",
            "id-rID",
            "in",
            "ja-rJP",
            "ko-rKR",
            "pl-rPL",
            "pt-rBR",
            "pt-rPT",
            "ru-rRU",
            "sk-rSK",
            "tr-rTR",
            "uk-rUA",
            "vi-rVN",
            "zh-rCN",
            "zh-rTW"
        )
    }
}
