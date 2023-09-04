package app.revanced.patches.youtube.misc.translations.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper.addTranslations

@Patch
@Name("Translations")
@Description("Add Crowdin translations for YouTube.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class TranslationsPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        context.addTranslations("youtube", LANGUAGE_LIST)

        SettingsPatch.updatePatchStatus("translations")

    }

    private companion object {
        val LANGUAGE_LIST = arrayOf(
            "ar",
            "az-rAZ",
            "be-rBY",
            "bg-rBG",
            "bn",
            "de-rDE",
            "el-rGR",
            "es-rES",
            "fi-rFI",
            "fr-rFR",
            "hu-rHU",
            "id-rID",
            "in",
            "it-rIT",
            "ja-rJP",
            "ko-rKR",
            "pl-rPL",
            "pt-rBR",
            "ro-rRO",
            "ru-rRU",
            "tr-rTR",
            "uk-rUA",
            "vi-rVN",
            "zh-rCN",
            "zh-rTW"
        )
    }
}
