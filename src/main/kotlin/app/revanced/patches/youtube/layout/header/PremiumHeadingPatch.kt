package app.revanced.patches.youtube.layout.header

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.resources.ResourceHelper.updatePatchStatusHeader
import kotlin.io.path.copyTo

@Patch(
    name = "Premium heading",
    description = "Show or hide the premium heading.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object PremiumHeadingPatch : ResourcePatch() {
    private const val DEFAULT_HEADING_RES = "yt_wordmark_header"
    private const val PREMIUM_HEADING_RES = "yt_premium_wordmark_header"

    private val UsePremiumHeading by booleanPatchOption(
        key = "UsePremiumHeading",
        default = true,
        title = "Use premium heading",
        description = "Whether to use the premium heading.",
        required = true
    )

    override fun execute(context: ResourceContext) {
        val resDirectory = context["res"]

        val (original, replacement) = if (UsePremiumHeading == true)
            PREMIUM_HEADING_RES to DEFAULT_HEADING_RES
        else
            DEFAULT_HEADING_RES to PREMIUM_HEADING_RES

        val variants = arrayOf("light", "dark")

        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).mapNotNull { dpi ->
            resDirectory.resolve("drawable-$dpi").takeIf { it.exists() }?.toPath()
        }.also {
            if (it.isEmpty())
                throw PatchException("The drawable folder can not be found. Therefore, the patch can not be applied.")
        }.forEach { path ->

            variants.forEach { mode ->
                val fromPath = path.resolve("${original}_$mode.png")
                val toPath = path.resolve("${replacement}_$mode.png")

                fromPath.copyTo(toPath, true)
            }
        }

        val header = if (UsePremiumHeading == true)
            "Premium"
        else
            "Default"

        context.updatePatchStatusHeader(header)
    }
}
