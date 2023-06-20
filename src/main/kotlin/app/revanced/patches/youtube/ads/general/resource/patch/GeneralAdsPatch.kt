package app.revanced.patches.youtube.ads.general.resource.patch

import app.revanced.extensions.doRecursively
import app.revanced.extensions.startsWithAny
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.ads.general.bytecode.patch.GeneralAdsBytecodePatch
import app.revanced.patches.youtube.ads.getpremium.patch.HideGetPremiumPatch
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceUtils.copyXmlNode
import org.w3c.dom.Element

@Patch
@Name("hide-general-ads")
@Description("Removes general ads.")
@DependsOn(
    [
        GeneralAdsBytecodePatch::class,
        HideGetPremiumPatch::class,
        LithoFilterPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class GeneralAdsPatch : ResourcePatch {
    private val resourceFileNames = arrayOf(
        "promoted_",
        "promotion_",
        "compact_premium_",
        "compact_promoted_",
        "simple_text_section",
    )

    private val replacements = arrayOf(
        "height",
        "width",
        "marginTop"
    )

    private val additionalReplacements = arrayOf(
        "Bottom",
        "End",
        "Start",
        "Top"
    )

    override fun execute(context: ResourceContext): PatchResult {
        context.forEach {

            if (!it.name.startsWithAny(*resourceFileNames)) return@forEach

            // for each file in the "layouts" directory replace all necessary attributes content
            context.xmlEditor[it.absolutePath].use { editor ->
                editor.file.doRecursively {
                    replacements.forEach replacement@{ replacement ->
                        if (it !is Element) return@replacement

                        it.getAttributeNode("android:layout_$replacement")?.let { attribute ->
                            attribute.textContent = "0.0dip"
                        }
                    }
                }
            }
        }

        context.xmlEditor["res/layout/simple_text_section.xml"].use { editor ->
            editor.file.doRecursively {
                additionalReplacements.forEach replacement@{ replacement ->
                    if (it !is Element) return@replacement

                    it.getAttributeNode("android:padding_$replacement")?.let { attribute ->
                        attribute.textContent = "0.0dip"
                    }
                }
            }
        }

        /**
         * Copy arrays
         */
        context.copyXmlNode("youtube/doubleback/host", "values/arrays.xml", "resources")

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: ADS_SETTINGS",
                "SETTINGS: HIDE_GENERAL_ADS",

                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_GENERAL_ADS",

                "PREFERENCE: GENERAL_SETTINGS",
                "PREFERENCE_HEADER: PLAYER",
                "SETTINGS: HIDE_AUDIO_TRACK_BUTTON",
                "SETTINGS: HIDE_VIEW_PRODUCT",
                "SETTINGS: DOUBLE_BACK_TIMEOUT",

                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "SETTINGS: COMMENT_COMPONENT_PARENT"
            )
        )

        SettingsPatch.updatePatchStatus("hide-general-ads")

        return PatchResultSuccess()
    }
}