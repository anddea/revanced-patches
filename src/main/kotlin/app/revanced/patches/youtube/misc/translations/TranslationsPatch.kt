package app.revanced.patches.youtube.misc.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.shared.translations.TranslationsUtils.copyXml
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

// Array of supported languages, each represented by its language code.
val LANGUAGES = arrayOf(
    "ar",
    "bg-rBG",
    "bn",
    "de-rDE",
    "el-rGR",
    "es-rES",
    "fi-rFI",
    "fr-rFR",
    "hu-rHU",
    "id-rID",
    "in",
    "it-rIT",
    "ja-rJP",
    "ko-rKR",
    "pl-rPL",
    "pt-rBR",
    "ru-rRU",
    "tr-rTR",
    "uk-rUA",
    "vi-rVN",
    "zh-rCN",
    "zh-rTW"
)

@Suppress("DEPRECATION", "unused")
object TranslationsPatch : BaseResourcePatch(
    name = "Translations",
    description = "Add Crowdin translations for YouTube.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private var CustomLanguage by stringPatchOption(
        key = "CustomLanguage",
        default = "",
        title = "Custom language file",
        description = """
            The file path to the strings.xml file. 
            Please note that applying the strings.xml file will overwrite all existing language translations.
            """
            .split("\n")
            .joinToString("\n") { it.trimIndent() } // Remove the leading whitespace from each line.
            .trimIndent(), // Remove the leading newline.
    )

    private var SelectedLanguages by stringPatchOption(
        key = "SelectedLanguages",
        default = LANGUAGES.joinToString(", "),
        title = "Selected languages",
        description = "",
    )

    override fun execute(context: ResourceContext) {
        CustomLanguage?.let { customLang ->
            if (customLang.isNotEmpty()) {
                try {
                    val customLangFile = File(customLang)
                    if (!customLangFile.exists()) {
                        throw PatchException("File not found: $customLang")
                    }
                    if (!customLangFile.isFile) {
                        throw PatchException("Provided path is not a file: $customLang")
                    }
                    if (customLangFile.name != "strings.xml") {
                        throw PatchException("Invalid file name: ${customLangFile.name}. Expected: strings.xml")
                    }
                    val resourceDirectory = context["res"]
                    val destinationDirectory = resourceDirectory.resolve("values")
                    val destinationFile = destinationDirectory.resolve("strings.xml")

                    updateStringsXml(customLangFile, destinationFile)
                } catch (e: Exception) {
                    throw PatchException("Error copying custom language file: ${e.message}")
                }
            }
            else {
                // Split the selected languages string into a list and filter the LANGUAGES array.
                val selectedLanguagesArray = SelectedLanguages!!
                    .split(",").map { it.trim() }.toTypedArray()
                val filteredLanguages = LANGUAGES.filter { it in selectedLanguagesArray }.toTypedArray()

                /**
                 * Copies XML translation files for the selected languages from the source directory.
                 *
                 * sourceDirectory The source directory containing the translation files.
                 * languageArray The array of language codes to process.
                 */
                context.copyXml(
                    "youtube",
                    filteredLanguages
                )
            }
        }

        SettingsPatch.updatePatchStatus(this)
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
}
