package app.revanced.patches.reddit.utils.settings.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.patch.IntegrationsPatch
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsBytecodePatch
import app.revanced.util.resources.ResourceUtils.copyXmlNode
import java.io.FileWriter
import java.nio.file.Files
import kotlin.io.path.exists

@Patch
@Name("reddit-settings")
@Description("Adds ReVanced settings to Reddit.")
@DependsOn(
    [
        IntegrationsPatch::class,
        SettingsBytecodePatch::class
    ]
)
@RedditCompatibility
@Version("0.0.1")
class SettingsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /**
         * For some reason, when you try to compile in ReVanced Manager you'll get an error
         * It is presumed to be an APKTOOL issue, but it cannot be fixed right now
         * So it has been changed so that settings can be added only through options.json
         */
        if (RedditSettings != true)
            return PatchResultSuccess()


        /**
         * Copy strings.xml
         */
        context.copyXmlNode("reddit/settings/host", "values/strings.xml", "resources")


        /**
         * Initialize settings activity
         */
        SettingsBytecodePatch.injectActivity()


        /**
         * Replace settings icon
         */
        arrayOf("preferences", "preferences_logged_in").forEach { targetXML ->
            val resDirectory = context["res"]
            val targetXml = resDirectory.resolve("xml").resolve("$targetXML.xml").toPath()

            if (!targetXml.exists())
                return PatchResultError("The preferences can not be found.")

            val preference = context["res/xml/$targetXML.xml"]

            preference.writeText(
                preference.readText()
                    .replace(
                        "\"@drawable/icon_text_post\" android:title=\"@string/label_acknowledgements\"",
                        "\"@drawable/icon_beta_planet\" android:title=\"@string/label_acknowledgements\""
                    )
            )
        }


        /**
         * Replace settings label
         */
        val resDirectory = context["res"]
        if (!resDirectory.isDirectory)
            return PatchResultError("The res folder can not be found.")

        arrayOf(
            "values",
            "values-de-rDE",
            "values-en-rXA",
            "values-es-rES",
            "values-es-rMX",
            "values-fr-rCA",
            "values-fr-rFR",
            "values-it-rIT",
            "values-nl-rNL",
            "values-pt-rBR",
            "values-pt-rPT",
            "values-sv-rSE"
        ).forEach { path ->
            val directory = resDirectory.resolve("$path-v21")
            Files.createDirectories(directory.toPath())

            FileWriter(directory.resolve("strings.xml")).use {
                it.write("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?><resources><string name=\"label_acknowledgements\">ReVanced Extended</string></resources>")
            }
        }

        return PatchResultSuccess()
    }

    companion object : OptionsContainer() {
        internal var RedditSettings: Boolean? by option(
            PatchOption.BooleanOption(
                key = "RedditSettings",
                default = false,
                title = "Add settings to Reddit",
                description = "Defaults to false to avoid exceptions in ReVanced Manager"
            )
        )
    }
}