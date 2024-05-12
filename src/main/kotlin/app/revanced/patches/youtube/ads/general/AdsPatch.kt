package app.revanced.patches.youtube.ads.general

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fix.doublebacktoclose.DoubleBackToClosePatch
import app.revanced.patches.youtube.utils.fix.swiperefresh.SwipeRefreshPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import app.revanced.util.patch.BaseResourcePatch
import app.revanced.util.startsWithAny
import org.w3c.dom.Element

@Suppress("DEPRECATION", "unused")
object AdsPatch : BaseResourcePatch(
    name = "Hide ads",
    description = "Adds options to hide ads.",
    dependencies = setOf(
        AdsBytecodePatch::class,
        DoubleBackToClosePatch::class,
        LithoFilterPatch::class,
        SettingsPatch::class,
        SwipeRefreshPatch::class,
        VideoAdsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
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

    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/AdsFilter;"

    override fun execute(context: ResourceContext) {
        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

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
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ADS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}