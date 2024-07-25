@file:Suppress("DEPRECATION", "MemberVisibilityCanBePrivate", "SpellCheckingInspection")

package app.revanced.util

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption
import app.revanced.patcher.util.DomFileEditor
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

val classLoader: ClassLoader = object {}.javaClass.classLoader

fun PatchOption<String>.valueOrThrow() = value
    ?: throw PatchException("Invalid patch option: $title.")

fun PatchOption<Int?>.valueOrThrow() = value
    ?: throw PatchException("Invalid patch option: $title.")

fun PatchOption<String>.lowerCaseOrThrow() = valueOrThrow()
    .lowercase()

fun PatchOption<String>.underBarOrThrow() = lowerCaseOrThrow()
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

fun Node.insertNode(tagName: String, targetNode: Node, block: Element.() -> Unit) {
    val child = ownerDocument.createElement(tagName)
    child.block()
    parentNode.insertBefore(child, targetNode)
}

fun String.startsWithAny(vararg prefixes: String): Boolean {
    for (prefix in prefixes)
        if (this.startsWith(prefix))
            return true

    return false
}

fun ResourceContext.copyFile(
    resourceGroup: List<ResourceGroup>,
    path: String,
    warning: String
): Boolean {
    resourceGroup.let { resourceGroups ->
        try {
            val filePath = File(path)
            val resourceDirectory = this["res"]

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

/**
 * Copy resources from the current class loader to the resource directory.
 *
 * @param sourceResourceDirectory The source resource directory name.
 * @param resources The resources to copy.
 */
fun ResourceContext.copyResources(
    sourceResourceDirectory: String,
    vararg resources: ResourceGroup,
) {
    val targetResourceDirectory = this["res"]

    for (resourceGroup in resources) {
        resourceGroup.resources.forEach { resource ->
            val resourceFile = "${resourceGroup.resourceDirectoryName}/$resource"

            inputStreamFromBundledResource(
                sourceResourceDirectory,
                resourceFile
            )?.let { inputStream ->
                Files.copy(
                    inputStream,
                    targetResourceDirectory.resolve(resourceFile).toPath(),
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
fun ResourceContext.copyResourcesWithRename(
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
        this.xmlEditor[targetFile.toString()].use { editor ->
            updatePathData(editor.file, pathData)
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
fun ResourceContext.copyXmlNode(
    resourceDirectory: String,
    targetResource: String,
    elementTag: String
) = inputStreamFromBundledResource(
    resourceDirectory,
    targetResource
)?.let { inputStream ->

    // Copy nodes from the resources node to the real resource node
    elementTag.copyXmlNode(
        this.xmlEditor[inputStream],
        this.xmlEditor["res/$targetResource"]
    ).close()
}

/**
 * Copies the specified node of the source [DomFileEditor] to the target [DomFileEditor].
 * @param source the source [DomFileEditor].
 * @param target the target [DomFileEditor]-
 * @return AutoCloseable that closes the target [DomFileEditor]s.
 */
fun String.copyXmlNode(source: DomFileEditor, target: DomFileEditor): AutoCloseable {
    val hostNodes = source.file.getElementsByTagName(this).item(0).childNodes

    val destinationResourceFile = target.file
    val destinationNode = destinationResourceFile.getElementsByTagName(this).item(0)

    for (index in 0 until hostNodes.length) {
        val node = hostNodes.item(index).cloneNode(true)
        destinationResourceFile.adoptNode(node)
        destinationNode.appendChild(node)
    }

    return AutoCloseable {
        source.close()
        target.close()
    }
}
