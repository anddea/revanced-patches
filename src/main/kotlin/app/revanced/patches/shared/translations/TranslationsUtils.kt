package app.revanced.patches.shared.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.util.inputStreamFromBundledResource
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
                inputStreamFromBundledResource("$sourceDirectory/translations", "$language/strings.xml")!!,
                this["res"].resolve("$directory/strings.xml").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}
