package app.revanced.patches.music.layout.translations

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.TRANSLATIONS_FOR_YOUTUBE_MUSIC
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addLinkPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.translations.APP_LANGUAGES
import app.revanced.patches.shared.translations.baseTranslationsPatch

// Array of supported translations, each represented by its language code.
private val SUPPORTED_TRANSLATIONS = setOf(
    "bg-rBG", "bn", "cs-rCZ", "el-rGR", "es-rES", "fr-rFR", "hu-rHU", "id-rID", "in", "it-rIT",
    "ja-rJP", "ko-rKR", "nl-rNL", "pl-rPL", "pt-rBR", "ro-rRO", "ru-rRU", "ta-rIN", "tr-rTR", "uk-rUA",
    "vi-rVN", "zh-rCN", "zh-rTW"
)

@Suppress("unused")
val translationsBytecodePatch = bytecodePatch {
    execute {
        addLinkPreference(
            CategoryType.MISC,
            "revanced_translations",
            "https://rvxtranslate.netlify.app/"
        )
    }
}

@Suppress("unused")
val translationsPatch = resourcePatch(
    TRANSLATIONS_FOR_YOUTUBE_MUSIC.title,
    TRANSLATIONS_FOR_YOUTUBE_MUSIC.summary,
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
            SUPPORTED_TRANSLATIONS, "music"
        )

        updatePatchStatus(TRANSLATIONS_FOR_YOUTUBE_MUSIC)

    }
}
