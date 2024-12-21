package app.revanced.util

import app.revanced.patcher.patch.Option
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.util.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val classLoader = object {}.javaClass.classLoader

@Suppress("UNCHECKED_CAST")
fun Patch<*>.getStringOptionValue(key: String) =
    options[key] as Option<String>

fun Option<String>.valueOrThrow() = value
    ?: throw PatchException("Invalid patch option: $title.")

fun Option<Int?>.valueOrThrow() = value
    ?: throw PatchException("Invalid patch option: $title.")

fun Option<String>.lowerCaseOrThrow() = valueOrThrow()
    .lowercase()

fun Option<String>.underBarOrThrow() = lowerCaseOrThrow()
    .replace(" ", "_")

fun Node.adoptChild(tagName: String, block: Element.() -> Unit) {
    val child = ownerDocument.createElement(tagName)
    child.block()
    appendChild(child)
}

fun Node.cloneNodes(parent: Node) {
    val node = cloneNode(true)
    parent.appendChild(node)
    parent.removeChild(this)
}

/**
 * Recursively traverse the DOM tree starting from the given root node.
 *
 * @param action function that is called for every node in the tree.
 */
fun Node.doRecursively(action: (Node) -> Unit) {
    action(this)
    for (i in 0 until this.childNodes.length) this.childNodes.item(i).doRecursively(action)
}

fun List<String>.getResourceGroup(fileNames: Array<String>) = map { directory ->
    ResourceGroup(
        directory, *fileNames
    )
}

private fun ResourcePatchContext.getMipMapPath(): String {
    var path: String
    document("AndroidManifest.xml").use { document ->
        val manifestElement = document.getNode("application") as Element
        val mipmapResourceFile = manifestElement.getAttribute("android:icon").split("/")[1]
        path = "res/mipmap-anydpi/$mipmapResourceFile.xml"
    }
    return path
}

private fun ResourcePatchContext.getAdaptiveIconResourceFile(tag: String): String {
    val path = getMipMapPath()
    document(path).use { document ->
        val adaptiveIcon = document
            .getElementsByTagName("adaptive-icon")
            .item(0) as Element

        val childNodes = adaptiveIcon.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node is Element && node.tagName == tag && node.hasAttribute("android:drawable")) {
                return node.getAttribute("android:drawable").split("/")[1]
            }
        }
        throw PatchException("Element not found: $tag")
    }
}

private fun ResourcePatchContext.getAdaptiveIconBackgroundResourceFile() =
    getAdaptiveIconResourceFile("background")

private fun ResourcePatchContext.getAdaptiveIconForegroundResourceFile() =
    getAdaptiveIconResourceFile("foreground")

private fun ResourcePatchContext.getAdaptiveIconMonoChromeResourceFile() =
    getAdaptiveIconResourceFile("monochrome")

fun ResourcePatchContext.copyAdaptiveIcon(
    adaptiveIconBackgroundFileName: String,
    adaptiveIconForegroundFileName: String,
    mipmapDirectories: List<String>,
    adaptiveIconMonoChromeFileName: String? = null,
) {
    mapOf(
        adaptiveIconBackgroundFileName to getAdaptiveIconBackgroundResourceFile(),
        adaptiveIconForegroundFileName to getAdaptiveIconForegroundResourceFile()
    ).forEach { (oldIconResourceFile, newIconResourceFile) ->
        if (oldIconResourceFile != newIconResourceFile) {
            mipmapDirectories.forEach {
                val mipmapDirectory = get("res").resolve(it)
                Files.copy(
                    mipmapDirectory
                        .resolve("$oldIconResourceFile.png")
                        .toPath(),
                    mipmapDirectory
                        .resolve("$newIconResourceFile.png")
                        .toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }

    if (adaptiveIconMonoChromeFileName != null &&
        adaptiveIconMonoChromeFileName != getAdaptiveIconMonoChromeResourceFile()
    ) {
        val drawableDirectory = get("res").resolve("drawable")
        Files.copy(
            drawableDirectory
                .resolve("$adaptiveIconMonoChromeFileName.xml")
                .toPath(),
            drawableDirectory
                .resolve("${getAdaptiveIconMonoChromeResourceFile()}.xml")
                .toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}

fun ResourcePatchContext.appendAppVersion(appVersion: String) {
    addEntryValues(
        "revanced_spoof_app_version_target_entries",
        "@string/revanced_spoof_app_version_target_entry_" + appVersion.replace(".", "_"),
        prepend = false
    )
    addEntryValues(
        "revanced_spoof_app_version_target_entry_values",
        appVersion,
        prepend = false
    )
}

fun ResourcePatchContext.addEntryValues(
    attributeName: String,
    attributeValue: String,
    path: String = "res/values/arrays.xml",
    prepend: Boolean = true,
) {
    document(path).use { document ->
        with(document) {
            val resourcesNode = getElementsByTagName("resources").item(0) as Element
            val newElement: Element = createElement("item")
            for (i in 0 until resourcesNode.childNodes.length) {
                val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                if (node.getAttribute("name") == attributeName) {
                    newElement.appendChild(createTextNode(attributeValue))

                    if (prepend) {
                        node.appendChild(newElement)
                    } else {
                        node.insertBefore(newElement, node.firstChild)
                    }
                }
            }
        }
    }
}

fun ResourcePatchContext.copyFile(
    resourceGroup: List<ResourceGroup>,
    path: String,
    warning: String
): Boolean {
    resourceGroup.let { resourceGroups ->
        try {
            val filePath = File(path)
            val resourceDirectory = get("res")

            resourceGroups.forEach { group ->
                val fromDirectory = filePath.resolve(group.resourceDirectoryName)
                val toDirectory = resourceDirectory.resolve(group.resourceDirectoryName)

                group.resources.forEach { iconFileName ->
                    Files.write(
                        toDirectory.resolve(iconFileName).toPath(),
                        fromDirectory.resolve(iconFileName).readBytes()
                    )
                }
            }

            return true
        } catch (_: Exception) {
            println(warning)
        }
    }
    return false
}

fun ResourcePatchContext.removeOverlayBackground(
    files: Array<String>,
    targetId: Array<String>,
) {
    files.forEach { file ->
        val resourceDirectory = get("res")
        val targetXmlPath = resourceDirectory.resolve("layout").resolve(file)

        if (targetXmlPath.exists()) {
            targetId.forEach { identifier ->
                document("res/layout/$file").use { document ->
                    document.doRecursively {
                        arrayOf("height", "width").forEach replacement@{ replacement ->
                            if (it !is Element) return@replacement

                            if (it.attributes.getNamedItem("android:id")?.nodeValue?.endsWith(
                                    identifier
                                ) == true
                            ) {
                                it.getAttributeNode("android:layout_$replacement")
                                    ?.let { attribute ->
                                        attribute.textContent = "0.0dip"
                                    }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun ResourcePatchContext.removeStringsElements(
    replacements: Array<String>
) {
    var languageList = emptyArray<String>()
    val resourceDirectory = get("res")
    val dir = resourceDirectory.listFiles()
    for (file in dir!!) {
        val path = file.name
        if (path.startsWith("values")) {
            val targetXml = resourceDirectory.resolve(path).resolve("strings.xml")
            if (targetXml.exists()) languageList += path
        }
    }
    removeStringsElements(languageList, replacements)
}

fun ResourcePatchContext.removeStringsElements(
    paths: Array<String>,
    replacements: Array<String>
) {
    paths.forEach { path ->
        val resourceDirectory = get("res")
        val targetXmlPath = resourceDirectory.resolve(path).resolve("strings.xml")

        if (targetXmlPath.exists()) {
            val targetXml = get("res/$path/strings.xml")

            replacements.forEach replacementsLoop@{ replacement ->
                targetXml.writeText(
                    targetXml.readText()
                        .replaceFirst(""" {4}<string name="$replacement".+""".toRegex(), "")
                )
            }
        }
    }
}

fun Node.insertNode(tagName: String, targetNode: Node, block: Element.() -> Unit) {
    val child = ownerDocument.createElement(tagName)
    child.block()
    parentNode.insertBefore(child, targetNode)
}

/**
 * Copy resources from the current class loader to the resource directory.
 *
 * @param sourceResourceDirectory The source resource directory name.
 * @param resources The resources to copy.
 */
fun ResourcePatchContext.copyResources(
    sourceResourceDirectory: String,
    vararg resources: ResourceGroup,
    createDirectoryIfNotExist: Boolean = false,
) {
    val resourceDirectory = get("res")

    for (resourceGroup in resources) {
        resourceGroup.resources.forEach { resource ->
            val resourceDirectoryName = resourceGroup.resourceDirectoryName
            if (createDirectoryIfNotExist) {
                val targetDirectory = resourceDirectory.resolve(resourceDirectoryName)
                if (!targetDirectory.isDirectory) Files.createDirectories(targetDirectory.toPath())
            }
            val resourceFile = "$resourceDirectoryName/$resource"
            inputStreamFromBundledResource(
                sourceResourceDirectory,
                resourceFile
            )?.let { inputStream ->
                Files.copy(
                    inputStream,
                    resourceDirectory.resolve(resourceFile).toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        }
    }
}

/**
 * Copy resources from the current class loader to the resource directory with the option to rename.
 *
 * @param sourceResourceDirectory The source resource directory name.
 * @param resourceMap The map containing resource titles and their respective path data.
 */
fun ResourcePatchContext.copyResourcesWithRename(
    sourceResourceDirectory: String,
    resourceMap: Map<String, String>
) {
    val targetResourceDirectory = this["res"]

    for ((title, pathData) in resourceMap) {
        // Check if pathData is another title
        if (resourceMap.containsKey(pathData)) {
            continue // Skip copying if the pathData is another title
        }

        val resourceFile = "drawable/icon.xml"
        val inputStream = inputStreamFromBundledResource(sourceResourceDirectory, resourceFile)!!
        val targetFile = targetResourceDirectory.resolve("drawable/$title.xml").toPath()

        Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING)

        // Update the XML with the new path data
        document(targetFile.toString()).use { document ->
            updatePathData(document, pathData)
        }
    }
}

/**
 * Update the `android:pathData` attribute in the XML document.
 *
 * @param document The XML document.
 * @param pathData The new path data to set.
 */
fun updatePathData(document: org.w3c.dom.Document, pathData: String) {
    val elements = document.getElementsByTagName("path")
    for (i in 0 until elements.length) {
        val pathElement = elements.item(i) as? Element
        pathElement?.setAttribute("android:pathData", pathData)
    }
}

internal fun inputStreamFromBundledResourceOrThrow(
    sourceResourceDirectory: String,
    resourceFile: String,
) = classLoader.getResourceAsStream("$sourceResourceDirectory/$resourceFile")
    ?: throw PatchException("Could not find $resourceFile")

internal fun inputStreamFromBundledResource(
    sourceResourceDirectory: String,
    resourceFile: String,
): InputStream? = classLoader.getResourceAsStream("$sourceResourceDirectory/$resourceFile")

/**
 * Resource names mapped to their corresponding resource data.
 * @param resourceDirectoryName The name of the directory of the resource.
 * @param resources A list of resource names.
 */
class ResourceGroup(val resourceDirectoryName: String, vararg val resources: String)

/**
 * Copy resources from the current class loader to the resource directory.
 * @param resourceDirectory The directory of the resource.
 * @param targetResource The target resource.
 * @param elementTag The element to copy.
 */
fun ResourcePatchContext.copyXmlNode(
    resourceDirectory: String,
    targetResource: String,
    elementTag: String
) = inputStreamFromBundledResource(
    resourceDirectory,
    targetResource
)?.let { inputStream ->
    // Copy nodes from the resources node to the real resource node
    elementTag.copyXmlNode(
        document(inputStream),
        document("res/$targetResource"),
    ).close()
}

/**
 * Copies the specified node of the source [Document] to the target [Document].
 * @param source the source [Document].
 * @param target the target [Document]-
 * @return AutoCloseable that closes the [Document]s.
 */
fun String.copyXmlNode(
    source: Document,
    target: Document,
): AutoCloseable {
    val hostNodes = source.getElementsByTagName(this).item(0).childNodes

    val destinationNode = target.getElementsByTagName(this).item(0)

    for (index in 0 until hostNodes.length) {
        val node = hostNodes.item(index).cloneNode(true)
        target.adoptNode(node)
        destinationNode.appendChild(node)
    }

    return AutoCloseable {
        source.close()
        target.close()
    }
}

internal fun org.w3c.dom.Document.getNode(tagName: String) =
    this.getElementsByTagName(tagName).item(0)

internal fun NodeList.findElementByAttributeValue(attributeName: String, value: String): Element? {
    for (i in 0 until length) {
        val node = item(i)
        if (node.nodeType == Node.ELEMENT_NODE) {
            val element = node as Element

            if (element.getAttribute(attributeName) == value) {
                return element
            }

            // Recursively search.
            val found = element.childNodes.findElementByAttributeValue(attributeName, value)
            if (found != null) {
                return found
            }
        }
    }

    return null
}

internal fun NodeList.findElementByAttributeValueOrThrow(attributeName: String, value: String) =
    findElementByAttributeValue(attributeName, value)
        ?: throw PatchException("Could not find: $attributeName $value")
