package app.revanced.patches.music.layout.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.translations.APP_LANGUAGES
import app.revanced.patches.shared.translations.TranslationsUtils.invoke
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object TranslationsPatch : BaseResourcePatch(
    name = "Translations for YouTube Music",
    description = "Add translations or remove string resources.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    // Array of supported translations, each represented by its language code.
    private val TRANSLATIONS = arrayOf(
        "bg-rBG", "bn", "cs-rCZ", "el-rGR", "es-rES", "fr-rFR", "hu-rHU", "id-rID", "in", "it-rIT",
        "ja-rJP", "ko-rKR", "nl-rNL", "pl-rPL", "pt-rBR", "ro-rRO", "ru-rRU", "tr-rTR", "uk-rUA",
        "vi-rVN", "zh-rCN", "zh-rTW"
    )

    private var CustomTranslation by stringPatchOption(
        key = "CustomTranslation",
        default = "",
        title = "Custom translation",
        description = """
            The path to the 'strings.xml' file.
            Please note that applying the 'strings.xml' file will overwrite all existing language translations.
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
            TRANSLATIONS, "music"
        )
    }
}
