package app.revanced.patches.youtube.layout.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.shared.translations.APP_LANGUAGES
import app.revanced.patches.shared.translations.TranslationsUtils.invoke
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element
import org.w3c.dom.Node

@Suppress("DEPRECATION", "unused")
object TranslationsPatch : BaseResourcePatch(
    name = "Translations for YouTube",
    description = "Add translations or remove string resources.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    // Array of supported translations, each represented by its language code.
    private val TRANSLATIONS = arrayOf(
        "ar",
        "bg-rBG",
        "de-rDE",
        "el-rGR",
        "es-rES",
        "fr-rFR",
        "hu-rHU",
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

    private var CustomTranslation by stringPatchOption(
        key = "CustomTranslation",
        default = "",
        title = "Custom translation",
        description = """
            The path to the 'strings.xml' file.
            Please note that applying the 'strings.xml' file will overwrite all existing translations.
            """.trimIndent(),
        required = true
    )

    private var SelectedTranslations by stringPatchOption(
        key = "SelectedTranslations",
        default = TRANSLATIONS.joinToString(", "),
        title = "Translations to add",
        description = "A list of translations to be added for the RVX settings, separated by commas.",
        required = true
    )

    private var SelectedStringResources by stringPatchOption(
        key = "SelectedStringResources",
        default = APP_LANGUAGES.joinToString(", "),
        title = "String resources to keep",
        description = """
            A list of string resources to be kept, separated by commas.
            String resources not in the list will be removed from the app.

            Default string resource, English, is not removed.
            """.trimIndent(),
        required = true
    )

    override fun execute(context: ResourceContext) {
        context.invoke(
            CustomTranslation, SelectedTranslations, SelectedStringResources,
            TRANSLATIONS, "youtube"
        )

        // Process selected app languages
        val selectedAppLanguagesArray = SelectedStringResources!!.split(",").map { it.trim() }.toTypedArray()

        // Filter the app languages to include both versions of locales (with and without 'r', en-rGB and en-GB)
        // and also handle locales with "b+" prefix
        val filteredAppLanguages = selectedAppLanguagesArray.flatMap { language ->
            setOf(language, language.replace("-r", "-"),
                language.replace("b+", "").replace("+", "-"))
        }.toTypedArray()

        // Remove unselected app languages from UI
        context.xmlEditor["res/xml/locales_config.xml"].use { editor ->
            val nodesToRemove = mutableListOf<Node>()

            editor.file.doRecursively loop@{
                if (it !is Element || it.tagName != "locale") return@loop

                it.getAttributeNode("android:name")?.let { attribute ->
                    if (attribute.textContent != "en" && attribute.textContent !in filteredAppLanguages) {
                        nodesToRemove.add(it)
                    }
                }
            }

            // Remove the collected nodes (avoids NullPointerException)
            for (node in nodesToRemove) {
                node.parentNode?.removeChild(node)
            }
        }

        SettingsPatch.updatePatchStatus("Translations")
    }
}
