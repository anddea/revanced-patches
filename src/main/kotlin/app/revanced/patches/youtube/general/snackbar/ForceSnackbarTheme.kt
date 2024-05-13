package app.revanced.patches.youtube.general.snackbar

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import app.revanced.util.insertNode
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element

@Suppress("DEPRECATION", "unused")
object ForceSnackbarTheme : BaseResourcePatch(
    name = "Force snackbar theme",
    description = "Force snackbar background color to match selected theme.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
) {
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
        description = "Specify a background color for the snackbar. You can specify hex color."
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
        description = "Specify a stroke color for the snackbar. You can specify hex color."
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

        SettingsPatch.updatePatchStatus(this)

    }
}
