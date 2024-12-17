package app.revanced.patches.youtube.layout.theme

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.MATERIALYOU
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusTheme
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.copyXmlNode
import org.w3c.dom.Element
import java.nio.file.Files

@Suppress("unused")
val materialYouPatch = resourcePatch(
    MATERIALYOU.title,
    MATERIALYOU.summary,
    false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedThemePatch,
        settingsPatch,
    )

    execute {
        fun patchXmlFile(
            fromDir: String,
            toDir: String,
            xmlFileName: String,
            parentNode: String,
            targetNode: String? = null,
            attribute: String,
            newValue: String
        ) {
            val resourceDirectory = get("res")
            val fromDirectory = resourceDirectory.resolve(fromDir)
            val toDirectory = resourceDirectory.resolve(toDir)

            if (!toDirectory.isDirectory) Files.createDirectories(toDirectory.toPath())

            val fromXmlFile = fromDirectory.resolve(xmlFileName)
            val toXmlFile = toDirectory.resolve(xmlFileName)

            if (!fromXmlFile.exists()) {
                return
            }

            if (!toXmlFile.exists()) {
                Files.copy(
                    fromXmlFile.toPath(),
                    toXmlFile.toPath()
                )
            }

            document("res/$toDir/$xmlFileName").use { document ->
                val parentList = document.getElementsByTagName(parentNode).item(0) as Element

                if (targetNode != null) {
                    for (i in 0 until parentList.childNodes.length) {
                        val node = parentList.childNodes.item(i) as? Element ?: continue

                        if (node.nodeName == targetNode && node.hasAttribute(attribute)) {
                            node.getAttributeNode(attribute).textContent = newValue
                        }
                    }
                } else {
                    if (parentList.hasAttribute(attribute)) {
                        parentList.getAttributeNode(attribute).textContent = newValue
                    }
                }
            }
        }

        patchXmlFile(
            "drawable",
            "drawable-night-v31",
            "new_content_dot_background.xml",
            "shape",
            "solid",
            "android:color",
            "@android:color/system_accent1_100"
        )
        patchXmlFile(
            "drawable",
            "drawable-night-v31",
            "new_content_dot_background_cairo.xml",
            "shape",
            "solid",
            "android:color",
            "@android:color/system_accent1_100"
        )
        patchXmlFile(
            "drawable",
            "drawable-v31",
            "new_content_dot_background.xml",
            "shape",
            "solid",
            "android:color",
            "@android:color/system_accent1_200"
        )
        patchXmlFile(
            "drawable",
            "drawable-v31",
            "new_content_dot_background_cairo.xml",
            "shape",
            "solid",
            "android:color",
            "@android:color/system_accent1_200"
        )
        patchXmlFile(
            "drawable",
            "drawable-v31",
            "new_content_count_background.xml",
            "shape",
            "solid",
            "android:color",
            "@android:color/system_accent1_100"
        )
        patchXmlFile(
            "drawable",
            "drawable-v31",
            "new_content_count_background_cairo.xml",
            "shape",
            "solid",
            "android:color",
            "@android:color/system_accent1_100"
        )
        patchXmlFile(
            "layout",
            "layout-v31",
            "new_content_count.xml",
            "TextView",
            null,
            "android:textColor",
            "@android:color/system_neutral1_900"
        )

        copyXmlNode("youtube/materialyou/host", "values-v31/colors.xml", "resources")

        updatePatchStatusTheme("MaterialYou")

        addPreference(MATERIALYOU)

    }
}
