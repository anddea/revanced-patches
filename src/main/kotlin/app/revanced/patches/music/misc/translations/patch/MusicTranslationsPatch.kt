package app.revanced.patches.youtube.misc.translations.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Patch
@DependsOn([MusicIntegrationsPatch::class, MusicSettingsPatch::class])
@Name("translations-music")
@Description("Add Crowdin Translations for YouTube Music")
@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicTranslationsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        val revanced_translations = "translate" to arrayOf(
                "ar-v21",
                "az-rAZ-v21",
                "bg-rBG-v21",
                "bn-rIN-v21",
                "bn-v21",
                "cs-rCZ-v21",
                "de-rDE-v21",
                "el-rGR-v21",
                "es-rES-v21",
                "fi-rFI-v21",
                "fr-rFR-v21",
                "hi-rIN-v21",
                "hu-rHU-v21",
                "id-rID-v21",
                "in-v21",
                "it-rIT-v21",
                "ja-rJP-v21",
                "kn-rIN-v21",
                "ko-rKR-v21",
                "ml-rIN-v21",
                "nl-rNL-v21",
                "pa-rIN-v21",
                "pl-rPL-v21",
                "pt-rBR-v21",
                "pt-rPT-v21",
                "ro-rRO-v21",
                "ru-rRU-v21",
                "sk-rSK-v21",
                "sv-rFI-v21",
                "sv-rSE-v21",
                "ta-rIN-v21",
                "th-v21",
                "tr-rTR-v21",
                "uk-rUA-v21",
                "vi-rVN-v21",
                "zh-rCN-v21",
                "zh-rTW-v21"
        )

        val TranslationsResources = arrayOf(revanced_translations)

        val classLoader = this.javaClass.classLoader
        TranslationsResources.forEach { (path, languageNames) ->
            languageNames.forEach { name ->
                val resDirectory = context["res"].resolve("values-$name")
                val relativePath = "values-$name/strings.xml"

                Files.createDirectory(resDirectory.toPath())
                Files.copy(
                        classLoader.getResourceAsStream("music/$path/$relativePath")!!,
                        context["res"].resolve(relativePath).toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        return PatchResultSuccess()
    }
}
