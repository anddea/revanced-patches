package app.revanced.patches.youtube.misc.translations.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("translations")
@Description("Add Crowdin Translations.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class TranslationsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        ResourceHelper.addTranslations(context, "youtube", LANGUAGE_LIST)

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
            "hi-rIN",
            "hu-rHU",
            "id-rID",
            "in",
            "it-rIT",
            "ja-rJP",
            "ko-rKR",
            "pl-rPL",
            "pt-rBR",
            "ru-rRU",
            "tr-rTR",
            "uk-rUA",
            "vi-rVN",
            "zh-rCN",
            "zh-rTW"
        )
    }
}
