package app.revanced.patches.shared.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.util.inputStreamFromBundledResource
import org.w3c.dom.Node
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@Suppress("DEPRECATION")
object TranslationsUtils {
    /**
     * Extension function to ResourceContext to copy XML translation files.
     *
     * @param sourceDirectory The source directory containing the translation files.
     * @param languageArray The array of language codes to process.
     */
    internal fun ResourceContext.copyXml(
        sourceDirectory: String,
        languageArray: Array<String>
    ) {
        languageArray.forEach { language ->
            val directory = "values-$language-v21"
            this["res/$directory"].mkdir()

            Files.copy(
                inputStreamFromBundledResource(
                    "$sourceDirectory/translations",
                    "$language/strings.xml"
                )!!,
                this["res"].resolve("$directory/strings.xml").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
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
    internal fun updateStringsXml(sourceFile: File, destinationFile: File) {
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
}

// Array of all possible app languages.
val APP_LANGUAGES = arrayOf(
    "af", "am", "ar", "ar-rXB", "as", "az",
    "b+es+419", "b+sr+Latn", "be", "bg", "bn", "bs",
    "ca", "cs",
    "da", "de",
    "el", "en-rAU", "en-rCA", "en-rGB", "en-rIN", "en-rXA", "en-rXC", "es", "es-rUS", "et", "eu",
    "fa", "fi", "fr", "fr-rCA",
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
