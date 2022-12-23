package app.revanced.patches.youtube.misc.settings.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.misc.integrations.patch.IntegrationsPatch
import app.revanced.patches.youtube.misc.manifest.patch.FixLocaleConfigErrorPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.patches.youtube.misc.settings.bytecode.patch.SettingsBytecodePatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.patches.settings.AbstractSettingsResourcePatch
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils
import app.revanced.shared.util.resources.ResourceUtils.copyResources
import org.w3c.dom.Element

@Patch
@Name("settings")
@Description("Applies mandatory patches to implement ReVanced settings into the application.")
@DependsOn(
    [
        FixLocaleConfigErrorPatch::class,
        IntegrationsPatch::class,
        SharedResourcdIdPatch::class,
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

        /*
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

        /*
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

        /*
         add settings
         */
        ResourceHelper.addSettings(
            context,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: EXTENDED_SETTINGS",
            "SETTINGS: ABOUT"
        )

        return PatchResultSuccess()
    }
}