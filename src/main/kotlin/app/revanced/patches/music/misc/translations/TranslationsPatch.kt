package app.revanced.patches.music.misc.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.translations.TranslationsUtils.copyXml
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object TranslationsPatch : BaseResourcePatch(
    name = "Translations",
    description = "Adds Crowdin translations for YouTube Music.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {
        context.copyXml(
            "music",
            arrayOf(
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
        )

    }
}
