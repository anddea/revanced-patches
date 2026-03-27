package app.morphe.patches.reddit.layout.branding.name

import app.morphe.patches.reddit.utils.compatibility.Constants
import app.morphe.patches.reddit.utils.fix.signature.spoofSignaturePatch
import app.morphe.patches.reddit.utils.patch.PatchList
import app.morphe.patches.reddit.utils.settings.updatePatchStatus
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.util.Utils.printInfo
import app.morphe.util.valueOrThrow
import java.io.FileWriter
import java.nio.file.Files

private const val ORIGINAL_APP_NAME = "Reddit"
private const val APP_NAME = "RVX Reddit"

@Suppress("unused")
val customBrandingNamePatch = resourcePatch(
    PatchList.CUSTOM_BRANDING_NAME_FOR_REDDIT.title,
    PatchList.CUSTOM_BRANDING_NAME_FOR_REDDIT.summary,
    false,
) {
    compatibleWith(Constants.COMPATIBLE_PACKAGE)

    dependsOn(spoofSignaturePatch)

    val appNameOption = stringOption(
        key = "appName",
        default = ORIGINAL_APP_NAME,
        values = mapOf(
            "Default" to APP_NAME,
            "Original" to ORIGINAL_APP_NAME,
        ),
        title = "App name",
        description = "The name of the app.",
        required = true
    )

    execute {
        val appName = appNameOption
            .valueOrThrow()

        if (appName == ORIGINAL_APP_NAME) {
            printInfo("App name will remain unchanged as it matches the original.")
            return@execute
        }

        val resDirectory = get("res")

        val valuesV24Directory = resDirectory.resolve("values-v24")
        if (!valuesV24Directory.isDirectory)
            Files.createDirectories(valuesV24Directory.toPath())

        val stringsXml = valuesV24Directory.resolve("strings.xml")

        if (!stringsXml.exists()) {
            FileWriter(stringsXml).use {
                it.write("<?xml version=\"1.0\" encoding=\"utf-8\"?><resources></resources>")
            }
        }

        document("res/values-v24/strings.xml").use { document ->
            mapOf(
                "app_name" to appName
            ).forEach { (k, v) ->
                val stringElement = document.createElement("string")

                stringElement.setAttribute("name", k)
                stringElement.textContent = v

                document.getElementsByTagName("resources").item(0).appendChild(stringElement)
            }
        }

        updatePatchStatus(PatchList.CUSTOM_BRANDING_NAME_FOR_REDDIT)
    }
}
