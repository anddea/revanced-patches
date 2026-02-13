package app.morphe.patches.shared.materialyou

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.util.FilesCompat
import org.w3c.dom.Element

private fun ResourcePatchContext.patchXmlFile(
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

    if (!toDirectory.isDirectory) toDirectory.mkdirs()

    val fromXmlFile = fromDirectory.resolve(xmlFileName)
    val toXmlFile = toDirectory.resolve(xmlFileName)

    if (!fromXmlFile.exists()) {
        return
    }

    if (!toXmlFile.exists()) {
        FilesCompat.copy(
            fromXmlFile,
            toXmlFile
        )
    }

    document("res/$toDir/$xmlFileName").use { document ->
        val parentList = document.getElementsByTagName(parentNode).item(0) as Element

        if (targetNode != null) {
            val childNodes = parentList.childNodes
            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i) as? Element ?: continue

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

fun ResourcePatchContext.baseMaterialYou() {
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
}