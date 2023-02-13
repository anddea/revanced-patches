package app.revanced.patches.music.misc.translations.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.resources.ResourceHelper

@Patch
@Name("translations-music")
@Description("Add Crowdin Translations for YouTube Music")
@DependsOn([MusicSettingsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicTranslationsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        ResourceHelper.addTranslations(context, "music", LANGUAGE_LIST)

        return PatchResultSuccess()
    }

    private companion object {
        val LANGUAGE_LIST = arrayOf(
            "az-rAZ",
            "be-rBY",
            "bn",
            "de-rDE",
            "el-rGR",
            "es-rES",
            "fr-rFR",
            "hi-rIN",
            "hu-rHU",
            "id-rID",
            "in",
            "it-rIT",
            "ja-rJP",
            "ko-rKR",
            "pl-rPL",
            "pt-rBR",
            "ru-rRU",
            "th-rTH",
            "tr-rTR",
            "uk-rUA",
            "zh-rCN",
            "zh-rTW"
        )
    }
}
