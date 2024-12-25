package app.revanced.patches.youtube.general.snackbar

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.FORCE_SNACKBAR_THEME
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.doRecursively
import app.revanced.util.insertNode
import org.w3c.dom.Element

private const val BACKGROUND = "?ytChipBackground"
private const val STROKE = "none"

@Suppress("unused")
val forceSnackbarTheme = resourcePatch(
    FORCE_SNACKBAR_THEME.title,
    FORCE_SNACKBAR_THEME.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    
    val cornerRadius by stringOption(
        key = "cornerRadius",
        default = "8.0dip",
        title = "Corner radius",
        description = "Specify a corner radius for the snackbar."
    )

    val backgroundColor by stringOption(
        key = "backgroundColor",
        default = BACKGROUND,
        values = mapOf(
            "Chip" to BACKGROUND,
            "Base" to "?ytBaseBackground"
        ),
        title = "Background color",
        description = "Specify a background color for the snackbar. You can specify hex color."
    )

    val strokeColor by stringOption(
        key = "strokeColor",
        default = STROKE,
        values = mapOf(
            "None" to STROKE,
            "Accent" to "?attr/colorAccent",
            "Inverted" to "?attr/ytInvertedBackground"
        ),
        title = "Stroke color",
        description = "Specify a stroke color for the snackbar. You can specify hex color."
    )

    execute {

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

            document(xmlPath).use { document ->
                document.doRecursively loop@{ node ->
                    if (node is Element && (tagName.isEmpty() || node.tagName == tagName)) {
                        setAttributes(node, *attributesAndValues)
                    }
                }
            }
        }

        fun insert(xmlPath: String, tagName: String, insertTagName: String, vararg attributesAndValues: String?) {
            require(attributesAndValues.size % 2 == 0) { "Number of attributes and values must be even." }

            document(xmlPath).use { document ->
                document.doRecursively loop@{ node ->
                    if (node is Element && node.tagName == insertTagName) {
                        node.insertNode(tagName, node) {
                            setAttributes(this, *attributesAndValues)
                        }
                    }
                }
            }
        }

        if (strokeColor != "none")
            insert(
                "res/drawable/snackbar_rounded_corners_background.xml",
                "stroke",
                "corners",
                "android:width",
                "1dp",
                "android:color",
                strokeColor
            )

        editXml(
            "res/drawable/snackbar_rounded_corners_background.xml", "corners",
            "android:bottomLeftRadius", cornerRadius,
            "android:bottomRightRadius", cornerRadius,
            "android:topLeftRadius", cornerRadius,
            "android:topRightRadius", cornerRadius
        )

        editXml("res/drawable/snackbar_rounded_corners_background.xml", "solid", "android:color", backgroundColor)

        try {
            listOf(
                "res/layout/inset_snackbar.xml", "res/layout/inset_youtube_snackbar.xml",
                "res/layout-sw600dp/inset_snackbar.xml", "res/layout-sw600dp/inset_youtube_snackbar.xml"
            )
                .forEach { editXml(it, "", "yt:messageTextColor", "?ytTextPrimary") }
        } catch (_: Exception) { /* Ignore the error in lower versions */ }

        addPreference(FORCE_SNACKBAR_THEME)

    }
}
