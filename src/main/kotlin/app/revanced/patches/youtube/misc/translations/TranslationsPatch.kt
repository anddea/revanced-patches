package app.revanced.patches.youtube.misc.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.shared.translations.APP_LANGUAGES
import app.revanced.patches.shared.translations.TranslationsUtils.copyXml
import app.revanced.patches.shared.translations.TranslationsUtils.updateStringsXml
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File

/**
 * Array of supported RVX languages, each represented by its language code.
 */
val LANGUAGES = arrayOf(
    "ar", "bg-rBG", "bn", "de-rDE", "el-rGR", "es-rES", "fi-rFI", "fr-rFR",
    "hu-rHU", "id-rID", "in", "it-rIT", "ja-rJP", "ko-rKR", "pl-rPL",
    "pt-rBR", "ru-rRU", "tr-rTR", "uk-rUA", "vi-rVN", "zh-rCN", "zh-rTW"
)

/**
 * The TranslationsPatch object adds Crowdin translations for YouTube.
 * This object extends BaseResourcePatch and provides functionality to patch
 * YouTube with custom or predefined language translations.
 */
@Suppress("DEPRECATION", "unused")
object TranslationsPatch : BaseResourcePatch(
    name = "Translations",
    description = "Add Crowdin translations for YouTube.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    /**
     * Patch option for specifying a custom language file.
     * The file path to the strings.xml file must be provided.
     * If set, this will overwrite all existing language translations.
     */
    private var CustomLanguage by stringPatchOption(
        key = "CustomLanguage",
        default = "",
        title = "Custom language file",
        description = """
            The file path to the strings.xml file.
            Please note that applying the strings.xml file will overwrite all existing language translations.
            """.trimIndent()
    )

    /**
     * Patch option for specifying selected RVX languages to be added.
     */
    private var SelectedLanguages by stringPatchOption(
        key = "SelectedLanguages",
        default = LANGUAGES.joinToString(", "),
        title = "Selected RVX languages",
        description = "Selected RVX languages that will be added."
    )

    /**
     * Patch option for specifying selected app languages to be kept.
     * Languages not in the list will be removed from the app.
     */
    private var SelectedAppLanguages by stringPatchOption(
        key = "SelectedAppLanguages",
        default = APP_LANGUAGES.joinToString(", "),
        title = "Selected app languages",
        description = "Selected app languages that will be kept, languages that are not in the list will be removed from the app."
    )

    /**
     * Executes the patch to add or update translations in the YouTube app.
     * This method handles both custom language files and predefined RVX languages,
     * as well as managing app languages to be kept or removed.
     */
    override fun execute(context: ResourceContext) {
        // Check if a custom language file is set
        CustomLanguage?.takeIf { it.isNotEmpty() }?.let { customLang ->
            try {
                val customLangFile = File(customLang)
                // Validate the custom language file
                if (!customLangFile.exists() || !customLangFile.isFile || customLangFile.name != "strings.xml") {
                    throw PatchException("Invalid custom language file: $customLang")
                }
                val resourceDirectory = context["res"].resolve("values")
                val destinationFile = resourceDirectory.resolve("strings.xml")

                // Update the strings.xml with the custom language file
                updateStringsXml(customLangFile, destinationFile)
            } catch (e: Exception) {
                throw PatchException("Error copying custom language file: ${e.message}")
            }
        } ?: run {
            // Process selected RVX languages if no custom language file is set
            val selectedLanguagesArray = SelectedLanguages!!.split(",").map { it.trim() }.toTypedArray()
            val filteredLanguages = LANGUAGES.filter { it in selectedLanguagesArray }.toTypedArray()
            context.copyXml("youtube", filteredLanguages)
        }

        // Process selected app languages
        val selectedAppLanguagesArray = SelectedAppLanguages!!.split(",").map { it.trim() }.toTypedArray()

        // Filter the app languages to include both versions of locales (with and without 'r', en-rGB and en-GB)
        // and also handle locales with "b+" prefix
        val filteredAppLanguages = selectedAppLanguagesArray.flatMap { language ->
            setOf(language, language.replace("-r", "-"),
                language.replace("b+", "").replace("+", "-"))
        }.toTypedArray()
        
        val resourceDirectory = context["res"]

        // Remove unselected app languages
        APP_LANGUAGES.filter { it !in filteredAppLanguages }.forEach { language ->
            resourceDirectory.resolve("values-$language").takeIf { it.exists() && it.isDirectory }?.deleteRecursively()
        }

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

        // Update the patch status
        SettingsPatch.updatePatchStatus(this)
    }
}
