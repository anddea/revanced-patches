package app.revanced.patches.music.misc.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.translations.APP_LANGUAGES
import app.revanced.patches.shared.translations.TranslationsUtils.copyXml
import app.revanced.patches.shared.translations.TranslationsUtils.updateStringsXml
import app.revanced.util.patch.BaseResourcePatch
import java.io.File

// Array of supported RVX languages, each represented by its language code.
val LANGUAGES = arrayOf(
    "bg-rBG", "bn", "cs-rCZ", "el-rGR", "es-rES", "fr-rFR", "hu-rHU", "id-rID", "in", "it-rIT",
    "ja-rJP", "ko-rKR", "nl-rNL", "pl-rPL", "pt-rBR", "ro-rRO", "ru-rRU", "tr-rTR", "uk-rUA",
    "vi-rVN", "zh-rCN", "zh-rTW"
)

@Suppress("DEPRECATION", "unused")
object TranslationsPatch : BaseResourcePatch(
    name = "Translations",
    description = "Adds Crowdin translations for YouTube Music.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private var CustomLanguage by stringPatchOption(
        key = "CustomLanguage",
        default = "",
        title = "Custom language file",
        description = """
            The file path to the strings.xml file.
            Please note that applying the strings.xml file will overwrite all existing language translations.
            """.trimIndent()
    )

    private var SelectedLanguages by stringPatchOption(
        key = "SelectedLanguages",
        default = LANGUAGES.joinToString(", "),
        title = "Selected RVX languages",
        description = "Selected RVX languages that will be added."
    )

    private var SelectedAppLanguages by stringPatchOption(
        key = "SelectedAppLanguages",
        default = APP_LANGUAGES.joinToString(", "),
        title = "Selected app languages",
        description = "Selected app languages that will be kept, languages that are not in the list will be removed from the app."
    )

    override fun execute(context: ResourceContext) {
        CustomLanguage?.takeIf { it.isNotEmpty() }?.let { customLang ->
            try {
                val customLangFile = File(customLang)
                if (!customLangFile.exists() || !customLangFile.isFile || customLangFile.name != "strings.xml") {
                    throw PatchException("Invalid custom language file: $customLang")
                }
                val resourceDirectory = context["res"].resolve("values")
                val destinationFile = resourceDirectory.resolve("strings.xml")

                updateStringsXml(customLangFile, destinationFile)
            } catch (e: Exception) {
                throw PatchException("Error copying custom language file: ${e.message}")
            }
        } ?: run {
            // Process selected RVX languages if no custom language file is set.
            val selectedLanguagesArray = SelectedLanguages!!.split(",").map { it.trim() }.toTypedArray()
            val filteredLanguages = LANGUAGES.filter { it in selectedLanguagesArray }.toTypedArray()
            context.copyXml("music", filteredLanguages)
        }

        // Process selected app languages.
        val selectedAppLanguagesArray = SelectedAppLanguages!!.split(",").map { it.trim() }.toTypedArray()
        val filteredAppLanguages = APP_LANGUAGES.filter { it in selectedAppLanguagesArray }.toTypedArray()
        val resourceDirectory = context["res"]

        // Remove unselected app languages.
        APP_LANGUAGES.filter { it !in filteredAppLanguages }.forEach { language ->
            resourceDirectory.resolve("values-$language").takeIf { it.exists() && it.isDirectory }?.deleteRecursively()
        }
    }
}
