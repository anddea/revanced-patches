package app.revanced.patches.youtube.layout.translations

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.shared.translations.APP_LANGUAGES
import app.revanced.patches.shared.translations.baseTranslationsPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.TRANSLATIONS_FOR_YOUTUBE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

// Array of supported translations, each represented by its language code.
private val SUPPORTED_TRANSLATIONS = setOf(
    "ar",
    "az-rAZ",
    "be-rBY",
    "bg-rBG",
    "cs-rCZ",
    "da-rDK",
    "de-rDE",
    "el-rGR",
    "en-rGB",
    "en-rUS",
    "es-rES",
    "es-rUS",
    "fa-rIR",
    "fil-rPH",
    "fr-rFR",
    "ga-rIE",
    "hu-rHU",
    "id-rID",
    "in",
    "it-rIT",
    "iw-rIL",
    "ja-rJP",
    "ko-rKR",
    "lo-rLA",
    "my-rMM",
    "nl-rNL",
    "pa-rIN",
    "pl-rPL",
    "pt-rBR",
    "ru-rRU",
    "sk-rSK",
    "sv-rSE",
    "ta-rIN",
    "tr-rTR",
    "uk-rUA",
    "ur-rPK",
    "uz-rUZ",
    "vi-rVN",
    "zh-rCN",
    "zh-rTW",
)

@Suppress("unused")
val translationsBytecodePatch = bytecodePatch {
    execute {
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: TRANSLATIONS"
            ), TRANSLATIONS_FOR_YOUTUBE
        )
    }
}

@Suppress("unused")
val translationsPatch = resourcePatch(
    TRANSLATIONS_FOR_YOUTUBE.title,
    TRANSLATIONS_FOR_YOUTUBE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)
    dependsOn(translationsBytecodePatch, settingsPatch)

    val customTranslations by stringOption(
        key = "customTranslations",
        default = "",
        title = "Custom translations",
        description = """
            The path to the 'strings.xml' file.
            Please note that applying the 'strings.xml' file will overwrite all existing translations.
            """.trimIndent(),
        required = true,
    )

    val selectedTranslations by stringOption(
        key = "selectedTranslations",
        default = SUPPORTED_TRANSLATIONS.joinToString(", "),
        title = "Translations to add",
        description = "A list of translations to be added for the RVX settings, separated by commas.",
        required = true,
    )

    val selectedStringResources by stringOption(
        key = "selectedStringResources",
        default = APP_LANGUAGES.joinToString(", "),
        title = "String resources to keep",
        description = """
            A list of string resources to be kept, separated by commas.
            String resources not in the list will be removed from the app.

            Default string resource, English, is not removed.
            """.trimIndent(),
        required = true,
    )

    execute {
        baseTranslationsPatch(
            customTranslations, selectedTranslations, selectedStringResources,
            SUPPORTED_TRANSLATIONS, "youtube"
        )

        addPreference(TRANSLATIONS_FOR_YOUTUBE)

    }
}
