package app.revanced.patches.youtube.general.snackbar

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import app.revanced.util.insertNode
import org.w3c.dom.Element

@Patch(
    name = "Force snackbar theme",
    description = "Force snackbar background color to match selected theme.",
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
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.43",
                "19.12.41",
                "19.13.37",
                "19.14.43",
                "19.15.36",
                "19.16.38"
            ]
        )
    ],
    use = true
)
@Suppress("unused")
object ForceSnackbarTheme : ResourcePatch() {
    private const val BACKGROUND = "?ytChipBackground"
    private const val STROKE = "none"

    private val CornerRadius by stringPatchOption(
        key = "CornerRadius",
        default = "8.0dip",
        title = "Corner radius",
        description = "Specify a corner radius for the snackbar."
    )

    private val BackgroundColor by stringPatchOption(
        key = "BackgroundColor",
        default = BACKGROUND,
        values = mapOf(
            "Chip" to BACKGROUND,
            "Base" to "?ytBaseBackground"
        ),
        title = "Background color",
        description = "Specify a background color for the snackbar."
    )

    private val StrokeColor by stringPatchOption(
        key = "StrokeColor",
        default = STROKE,
        values = mapOf(
            "None" to STROKE,
            "Accent" to "?attr/colorAccent",
            "Inverted" to "?attr/ytInvertedBackground"
        ),
        title = "Stroke color",
        description = "Specify a stroke color for the snackbar."
    )

    override fun execute(context: ResourceContext) {

        fun setAttributes(node: Element, vararg attributesAndValues: String?) {
            for (i in attributesAndValues.indices step 2) {
                val attribute = attributesAndValues[i]
                val value = attributesAndValues[i + 1]
                if (attribute != null && value != null) {
                    node.setAttribute(attribute, value)
                }
            }
        }

        fun editXml(xmlPath: String, tagName: String, vararg attributesAndValues: String?) {
            require(attributesAndValues.size % 2 == 0) { "Number of attributes and values must be even." }

            context.xmlEditor[xmlPath].use { editor ->
                editor.file.doRecursively { node ->
                    if (node is Element && (tagName.isEmpty() || node.tagName == tagName)) {
                        setAttributes(node, *attributesAndValues)
                    }
                }
            }
        }

        fun insert(xmlPath: String, tagName: String, insertTagName: String, vararg attributesAndValues: String?) {
            require(attributesAndValues.size % 2 == 0) { "Number of attributes and values must be even." }

            context.xmlEditor[xmlPath].use { editor ->
                editor.file.doRecursively { node ->
                    if (node is Element && node.tagName == insertTagName) {
                        node.insertNode(tagName, node) {
                            setAttributes(this, *attributesAndValues)
                        }
                    }
                }
            }
        }

        if (StrokeColor?.lowercase() != "none")
            insert("res/drawable/snackbar_rounded_corners_background.xml", "stroke", "corners", "android:width", "1dp", "android:color", StrokeColor)

        editXml("res/drawable/snackbar_rounded_corners_background.xml", "corners",
            "android:bottomLeftRadius", CornerRadius,
            "android:bottomRightRadius", CornerRadius,
            "android:topLeftRadius", CornerRadius,
            "android:topRightRadius", CornerRadius
        )

        editXml("res/drawable/snackbar_rounded_corners_background.xml", "solid", "android:color", BackgroundColor)

        listOf("res/layout/inset_snackbar.xml", "res/layout/inset_youtube_snackbar.xml",
            "res/layout-sw600dp/inset_snackbar.xml", "res/layout-sw600dp/inset_youtube_snackbar.xml")
            .forEach { editXml(it, "", "yt:messageTextColor", "?ytTextPrimary") }

        SettingsPatch.updatePatchStatus("Force snackbar theme")

    }
}
