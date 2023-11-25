package app.revanced.patches.music.misc.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.resources.ResourceHelper.addTranslations

@Patch(
    name = "Translations",
    description = "Add Crowdin translations for YouTube Music.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ]
)
@Suppress("unused")
object TranslationsPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        context.addTranslations("music", LANGUAGE_LIST)

    }

    private val LANGUAGE_LIST = arrayOf(
        "bg-rBG",
        "bn",
        "cs-rCZ",
        "el-rGR",
        "es-rES",
        "fr-rFR",
        "id-rID",
        "in",
        "it-rIT",
        "ja-rJP",
        "ko-rKR",
        "nl-rNL",
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
