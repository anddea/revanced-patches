package app.revanced.patches.reddit.layout.branding.name

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.patch.BaseResourcePatch
import java.io.FileWriter
import java.nio.file.Files

@Suppress("DEPRECATION", "unused")
object CustomBrandingNamePatch : BaseResourcePatch(
    name = "Custom branding name Reddit",
    description = "Renames the Reddit app to the name specified in options.json.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    private const val ORIGINAL_APP_NAME = "Reddit"
    private const val APP_NAME = "RVX Reddit"

    private val AppName by stringPatchOption(
        key = "AppName",
        default = ORIGINAL_APP_NAME,
        values = mapOf(
            "Default" to APP_NAME,
            "Original" to ORIGINAL_APP_NAME,
        ),
        title = "App name",
        description = "The name of the app."
    )

    override fun execute(context: ResourceContext) {
        val appName = if (AppName != null) {
            AppName!!
        } else {
            println("WARNING: Invalid name name. Does not apply patches.")
            ORIGINAL_APP_NAME
        }

        if (appName != ORIGINAL_APP_NAME) {
            val resDirectory = context["res"]

            val valuesV24Directory = resDirectory.resolve("values-v24")
            if (!valuesV24Directory.isDirectory)
                Files.createDirectories(valuesV24Directory.toPath())

            val stringsXml = valuesV24Directory.resolve("strings.xml")

            if (!stringsXml.exists()) {
                FileWriter(stringsXml).use {
                    it.write("<?xml version=\"1.0\" encoding=\"utf-8\"?><resources></resources>")
                }
            }

            context.xmlEditor["res/values-v24/strings.xml"].use { editor ->
                val document = editor.file

                mapOf(
                    "app_name" to appName
                ).forEach { (k, v) ->
                    val stringElement = document.createElement("string")

                    stringElement.setAttribute("name", k)
                    stringElement.textContent = v

                    document.getElementsByTagName("resources").item(0).appendChild(stringElement)
                }
            }
        } else {
            println("INFO: App name will remain unchanged as it matches the original.")
        }
    }
}
