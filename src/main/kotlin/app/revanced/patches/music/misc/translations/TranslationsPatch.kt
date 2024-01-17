package app.revanced.patches.music.misc.translations

import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.patch.translations.AbstractTranslationsPatch

@Patch(
    name = "Translations",
    description = "Adds Crowdin translations for YouTube Music.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object TranslationsPatch  : AbstractTranslationsPatch(
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
