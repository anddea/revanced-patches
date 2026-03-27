package app.morphe.patches.reddit.layout.translations

import app.morphe.patches.reddit.utils.compatibility.Constants
import app.morphe.patches.reddit.utils.patch.PatchList
import app.morphe.patches.reddit.utils.settings.settingsPatch
import app.morphe.patches.reddit.utils.settings.updatePatchStatus
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.translations.APP_LANGUAGES
import app.morphe.patches.shared.translations.baseTranslationsPatch

// Array of supported translations, each represented by its language code.
private val SUPPORTED_TRANSLATIONS = setOf(
    "ar", "az-rAZ", "bg-rBG", "da-rDK", "de-rDE", "el-rGR", "es-rES", "fil-rPH", "fr-rFR", "hu-rHU", "id-rID", "in", "it-rIT", "iw-rIL", "ja-rJP",
    "ko-rKR", "pl-rPL", "pt-rBR", "ru-rRU", "tr-rTR", "uk-rUA", "vi-rVN", "zh-rCN", "zh-rTW"
)

@Suppress("unused")
val translationsBytecodePatch = bytecodePatch {
    execute {
        updatePatchStatus(
            "enableTranslations",
            PatchList.TRANSLATIONS_FOR_REDDIT
        )
    }
}

@Suppress("unused")
val translationsPatch = resourcePatch(
    PatchList.TRANSLATIONS_FOR_REDDIT.title,
    PatchList.TRANSLATIONS_FOR_REDDIT.summary,
) {
    compatibleWith(Constants.COMPATIBLE_PACKAGE)
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
            SUPPORTED_TRANSLATIONS, "reddit"
        )

        // Enable RVX translations for more languages
        try {
            val localeConfigFile = "res/xml/_generated_res_locale_config.xml"
            if (get(localeConfigFile).exists()) {
                document(localeConfigFile).use { document ->
                    SUPPORTED_TRANSLATIONS.forEach {
                        val localeElement = document.createElement("locale")
                        val formattedLocale = it.replace("-r", "-")

                        localeElement.setAttribute("android:name", formattedLocale)
                        document.getElementsByTagName("locale-config").item(0).appendChild(localeElement)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
