package app.morphe.patches.shared.translations

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.util.FilesCompat
import app.morphe.util.doRecursively
import app.morphe.util.inputStreamFromBundledResource
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

// Array of all possible app languages.
val APP_LANGUAGES = arrayOf(
    "af", "am", "ar", "ar-rXB", "as", "az",
    "b+es+419", "b+sr+Latn", "be", "bg", "bn", "bs",
    "ca", "cs",
    "da", "de",
    "el", "en-rAU", "en-rCA", "en-rGB", "en-rIN", "en-rXA", "en-rXC", "es", "es-rUS", "et", "eu",
    "fa", "fi", "fil-rPH", "fr", "fr-rCA",
    "gl", "gu",
    "hi", "hr", "hu", "hy",
    "id", "in", "is", "it", "iw",
    "ja",
    "ka", "kk", "km", "kn", "ko", "ky",
    "lo", "lt", "lv",
    "mk", "ml", "mn", "mr", "ms", "my",
    "nb", "ne", "nl", "no",
    "or",
    "pa", "pl", "pt", "pt-rBR", "pt-rPT",
    "ro", "ru",
    "si", "sk", "sl", "sq", "sr", "sv", "sw",
    "ta", "te", "th", "tl", "tr",
    "uk", "ur", "uz",
    "vi",
    "zh", "zh-rCN", "zh-rHK", "zh-rTW", "zu",
)

fun ResourcePatchContext.baseTranslationsPatch(
    customTranslations: String?,
    selectedTranslations: String?,
    selectedStringResources: String?,
    translationsArray: Set<String>,
    sourceDirectory: String,
) {
    val resourceDirectory = get("res")
    val isYouTube = sourceDirectory == "youtube"

    // Check if the custom translation path is valid.
    customTranslations?.takeIf { it.isNotEmpty() }?.let { customLang ->
        try {
            val customLangFile = File(customLang)
            if (!customLangFile.exists() || !customLangFile.isFile || customLangFile.name != "strings.xml") {
                throw PatchException("Invalid custom language file: $customLang")
            }
            val valuesDirectory = resourceDirectory.resolve("values")
            val destinationFile = valuesDirectory.resolve("strings.xml")

            updateStringsXml(customLangFile, destinationFile)
        } catch (_: Exception) {
            // Exception is thrown if an invalid path is used in the patch option.
            throw PatchException("Invalid custom translations path:  $customLang")
        }
    } ?: run {
        // Process selected translations if no custom translation is set.
        val selectedTranslationsArray =
            selectedTranslations?.split(",")?.map { it.trim() }?.toTypedArray()
                ?: throw PatchException("Invalid selected languages.")
        val filteredLanguages =
            translationsArray.filter { it in selectedTranslationsArray }.toTypedArray()
        copyStringsXml(sourceDirectory, filteredLanguages)
    }

    // Process selected string resources.
    val selectedStringResourcesArray =
        selectedStringResources?.split(",")?.map { it.trim() }?.toTypedArray()
            ?: throw PatchException("Invalid selected string resources.")
    val filteredStringResources =
        APP_LANGUAGES.filter { it in selectedStringResourcesArray }.toTypedArray()

    // Remove unselected app languages.
    APP_LANGUAGES.filter { it !in filteredStringResources }.forEach { language ->
        resourceDirectory.resolve("values-$language").takeIf { it.exists() && it.isDirectory }
            ?.deleteRecursively()
    }

    // Filter the app languages to include both versions of locales (with and without 'r', en-rGB and en-GB)
    // and also handle locales with "b+" prefix
    var filteredAppLanguages = (selectedStringResourcesArray + arrayOf("en"))
        .map { language ->
            language.replace("-r", "-").replace("b+", "").replace("+", "-")
        }.toHashSet().toTypedArray()

    // Remove unselected app languages from UI
    try {
        document("res/xml/locales_config.xml").use { document ->
            val nodesToRemove = mutableListOf<Node>()

            document.doRecursively { node ->
                if (node is Element && node.tagName == "locale") {
                    node.getAttributeNode("android:name")?.let { attribute ->
                        if (attribute.textContent !in filteredAppLanguages) {
                            nodesToRemove.add(node)
                        }
                    }
                }
            }

            // Remove the collected nodes (avoids NullPointerException)
            for (node in nodesToRemove) {
                node.parentNode?.removeChild(node)
            }
        }
    } catch (_: Exception) {}

    if (!isYouTube) return

    filteredAppLanguages = filteredAppLanguages.map { language ->
        val hyphenIndex = language.indexOf("-") - 1
        if (hyphenIndex > 2) {
            language.subSequence(0, hyphenIndex).toString().uppercase()
        } else {
            language.uppercase()
        }
    }.toHashSet().toTypedArray()

    // Remove unselected app languages from RVX Settings
    document("res/values/arrays.xml").use { document ->
        val targetAttributeNames = setOf(
            "revanced_language_entries",
            "revanced_language_entry_values",
        )
        val nodesToRemove = mutableListOf<Node>()

        val resourcesNode = document.documentElement
        val childNodes = resourcesNode.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i) as? Element ?: continue

            if (node.getAttribute("name") in targetAttributeNames) {
                val itemNodes = node.childNodes
                for (j in 0 until itemNodes.length) {
                    val item = itemNodes.item(j) as? Element ?: continue
                    val text = item.textContent
                    val length = text.length
                    if (!text.endsWith("DEFAULT") &&
                        length >= 2 &&
                        text.subSequence(length - 2, length) !in filteredAppLanguages
                    ) {
                        nodesToRemove.add(item)
                    }
                }
            }
        }

        // Remove the collected nodes (avoids NullPointerException)
        for (n in nodesToRemove) {
            n.parentNode?.removeChild(n)
        }
    }
}

/**
 * Extension function to ResourceContext to copy XML translation files.
 *
 * @param sourceDirectory The source directory containing the translation files.
 * @param languageArray The array of language codes to process.
 */
private fun ResourcePatchContext.copyStringsXml(
    sourceDirectory: String,
    languageArray: Array<String>
) {
    val languageMap = mapOf(
        "fil-rPH" to "tl"
    )

    val resourceDirectory = get("res")
    languageArray.forEach { language ->
        val sourceLanguage = languageMap[language] ?: language

        inputStreamFromBundledResource(
            "$sourceDirectory/translations",
            "$language/strings.xml"
        )?.let { inputStream ->
            val directory = "values-$sourceLanguage-v21"
            val valuesV21Directory = resourceDirectory.resolve(directory)
            if (!valuesV21Directory.isDirectory) valuesV21Directory.mkdirs()

            FilesCompat.copy(
                inputStream,
                resourceDirectory.resolve("$directory/strings.xml")
            )
        }
    }
}

/**
 * Updates the contents of the destination strings.xml file by merging it with the source strings.xml file.
 *
 * This function reads both source and destination XML files, compares each <string> element by their
 * unique "name" attribute, and if a match is found, it replaces the content in the destination file with
 * the content from the source file.
 *
 * @param sourceFile The source strings.xml file containing new string values.
 * @param destinationFile The destination strings.xml file to be updated with values from the source file.
 */
private fun updateStringsXml(sourceFile: File, destinationFile: File) {
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    val documentBuilder = documentBuilderFactory.newDocumentBuilder()

    // Parse the source and destination XML files into Document objects
    val sourceDoc = documentBuilder.parse(sourceFile)
    val destinationDoc = documentBuilder.parse(destinationFile)

    val sourceStrings = sourceDoc.getElementsByTagName("string")
    val destinationStrings = destinationDoc.getElementsByTagName("string")

    // Create a map to store the <string> elements from the source document by their "name" attribute
    val sourceMap = mutableMapOf<String, Node>()

    // Populate the map with nodes from the source document
    for (i in 0 until sourceStrings.length) {
        val node = sourceStrings.item(i)
        val name = node.attributes.getNamedItem("name").nodeValue
        sourceMap[name] = node
    }

    // Update the destination document with values from the source document
    for (i in 0 until destinationStrings.length) {
        val node = destinationStrings.item(i)
        val name = node.attributes.getNamedItem("name").nodeValue
        if (sourceMap.containsKey(name)) {
            node.textContent = sourceMap[name]?.textContent
        }
    }

    /**
     * Prepare the transformer for writing the updated document back to the file.
     * The transformer is configured to indent the output XML for better readability.
     */
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

    val domSource = DOMSource(destinationDoc)
    val streamResult = StreamResult(destinationFile)
    transformer.transform(domSource, streamResult)
}
