package app.revanced.patches.music.misc.translations.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper.addTranslations

@Patch
@Name("Translations")
@Description("Add Crowdin translations for YouTube Music.")
@DependsOn([SettingsPatch::class])
@MusicCompatibility
class TranslationsPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        context.addTranslations("music", LANGUAGE_LIST)

    }

    private companion object {
        val LANGUAGE_LIST = arrayOf(
            "az-rAZ",
            "be-rBY",
            "bn",
            "cs-rCZ",
            "de-rDE",
            "el-rGR",
            "es-rES",
            "fr-rFR",
            "hi-rIN",
            "hu-rHU",
            "id-rID",
            "in",
            "it-rIT",
            "ja-rJP",
            "ko-rKR",
            "nl-rNL",
            "pl-rPL",
            "pt-rBR",
            "ru-rRU",
            "th-rTH",
            "tr-rTR",
            "uk-rUA",
            "vi-rVN",
            "zh-rCN",
            "zh-rTW"
        )
    }
}
