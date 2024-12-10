package app.revanced.patches.youtube.layout.translations

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.shared.translations.APP_LANGUAGES
import app.revanced.patches.shared.translations.baseTranslationsPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.TRANSLATIONS_FOR_YOUTUBE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.doRecursively
import org.w3c.dom.Element
import org.w3c.dom.Node

// Array of supported translations, each represented by its language code.
private val SUPPORTED_TRANSLATIONS = setOf(
    "ar", "bg-rBG", "de-rDE", "el-rGR", "es-rES", "fr-rFR", "hu-rHU", "it-rIT", "ja-rJP", "ko-rKR",
    "pl-rPL", "pt-rBR", "ru-rRU", "tr-rTR", "uk-rUA", "vi-rVN", "zh-rCN", "zh-rTW"
)

@Suppress("unused")
val translationsPatch = resourcePatch(
    TRANSLATIONS_FOR_YOUTUBE.title,
    TRANSLATIONS_FOR_YOUTUBE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

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

        // Process selected app languages
        val selectedAppLanguagesArray = selectedStringResources!!.split(",").map { it.trim() }.toTypedArray()

        // Filter the app languages to include both versions of locales (with and without 'r', en-rGB and en-GB)
        // and also handle locales with "b+" prefix
        val filteredAppLanguages = selectedAppLanguagesArray.flatMap { language ->
            setOf(language, language.replace("-r", "-"),
                language.replace("b+", "").replace("+", "-"))
        }.toTypedArray()

        // Remove unselected app languages from UI
        document("res/xml/locales_config.xml").use { document ->
            val nodesToRemove = mutableListOf<Node>()

            document.doRecursively loop@{ node ->
                if (node !is Element || node.tagName != "locale") return@loop

                node.getAttributeNode("android:name")?.let { attribute ->
                    if (attribute.textContent != "en" && attribute.textContent !in filteredAppLanguages) {
                        nodesToRemove.add(node)
                    }
                }
            }

            // Remove the collected nodes (avoids NullPointerException)
            for (node in nodesToRemove) {
                node.parentNode?.removeChild(node)
            }
        }

        addPreference(TRANSLATIONS_FOR_YOUTUBE)

    }
}
