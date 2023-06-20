package app.revanced.patches.youtube.utils.settings.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.patch.settings.AbstractSettingsResourcePatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.integrations.patch.IntegrationsPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.bytecode.patch.SettingsBytecodePatch
import app.revanced.util.resources.IconHelper.YOUTUBE_LAUNCHER_ICON_ARRAY
import app.revanced.util.resources.IconHelper.copyFiles
import app.revanced.util.resources.IconHelper.makeDirectoryAndCopyFiles
import app.revanced.util.resources.ResourceHelper.addPreference
import app.revanced.util.resources.ResourceHelper.addReVancedPreference
import app.revanced.util.resources.ResourceHelper.updatePatchStatus
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import org.w3c.dom.Element
import java.io.File
import java.nio.file.Paths

@Patch
@Name("settings")
@Description("Applies mandatory patches to implement ReVanced settings into the application.")
@DependsOn(
    [
        IntegrationsPatch::class,
        SharedResourceIdPatch::class,
        SettingsBytecodePatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SettingsPatch : AbstractSettingsResourcePatch(
    "youtube/settings",
    "youtube/settings/host",
    true
) {
    override fun execute(context: ResourceContext): PatchResult {
        super.execute(context)
        contexts = context

        /**
         * create directory for the untranslated language resources
         */
        context["res/values-v21"].mkdirs()

        arrayOf(
            ResourceUtils.ResourceGroup(
                "layout",
                "revanced_settings_toolbar.xml",
                "revanced_settings_with_toolbar.xml",
                "revanced_settings_with_toolbar_layout.xml"
            ),
            ResourceUtils.ResourceGroup(
                "values-v21",
                "strings.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/settings", resourceGroup)
        }

        /**
         * initialize ReVanced Settings
         */
        addReVancedPreference("extended_settings")

        /**
         * remove revanced settings divider
         */
        arrayOf("Theme.YouTube.Settings", "Theme.YouTube.Settings.Dark").forEach { themeName ->
            context.xmlEditor["res/values/styles.xml"].use { editor ->
                with(editor.file) {
                    val resourcesNode = getElementsByTagName("resources").item(0) as Element

                    val newElement: Element = createElement("item")
                    newElement.setAttribute("name", "android:listDivider")

                    for (i in 0 until resourcesNode.childNodes.length) {
                        val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                        if (node.getAttribute("name") == themeName) {
                            newElement.appendChild(createTextNode("@null"))

                            node.appendChild(newElement)
                        }
                    }
                }
            }
        }

        /**
         * If ad services config exists, disable it
         */
        context.xmlEditor["AndroidManifest.xml"].use { editor ->
            val tags = editor.file.getElementsByTagName("property")
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:name").contains("AD_SERVICES_CONFIG") }
                .forEach { it.parentNode.removeChild(it) }
        }

        /**
         * If a custom branding icon path exists, merge it
         */
        val iconPath = "branding"
        val targetDirectory = Paths.get("").toAbsolutePath().toString() + "/$iconPath"

        if (File(targetDirectory).exists()) {
            fun copyResources(resourceGroups: List<ResourceUtils.ResourceGroup>) {
                try {
                    context.copyFiles(resourceGroups, iconPath)
                } catch (_: Exception) {
                    context.makeDirectoryAndCopyFiles(resourceGroups, iconPath)
                }
            }

            val iconResourceFileNames =
                YOUTUBE_LAUNCHER_ICON_ARRAY
                    .map { "$it.png" }
                    .toTypedArray()

            fun createGroup(directory: String) = ResourceUtils.ResourceGroup(
                directory, *iconResourceFileNames
            )

            arrayOf("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi")
                .map { "mipmap-$it" }
                .map(::createGroup)
                .let(::copyResources)
        }

        return PatchResultSuccess()
    }

    companion object {
        internal lateinit var contexts: ResourceContext

        internal fun addPreference(settingArray: Array<String>) {
            contexts.addPreference(settingArray)
        }

        internal fun addReVancedPreference(key: String) {
            contexts.addReVancedPreference(key)
        }

        internal fun updatePatchStatus(patchTitle: String) {
            contexts.updatePatchStatus(patchTitle)
        }
    }
}