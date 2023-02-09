package app.revanced.patches.music.misc.settings.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.util.resources.ResourceUtils.copyXmlNode
import org.w3c.dom.Element

@Patch
@Name("music-settings")
@Description("Adds settings for ReVanced to YouTube Music.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicSettingsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         * Copy strings
         */

        context.copyXmlNode("music/settings/host", "values/strings.xml", "resources")

        /*
         * Copy colors
         */

        context.xmlEditor["res/values/colors.xml"].use { editor ->
            val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

            for (i in 0 until resourcesNode.childNodes.length) {
                val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                node.textContent = when (node.getAttribute("name")) {
                    "material_deep_teal_500" -> "@android:color/white"

                    else -> continue
                }
            }
        }

        /*
         * Copy preference fragments
         */

        context.copyXmlNode("music/settings/host", "xml/settings_headers.xml", "PreferenceScreen")

        // Removed since YouTube Music v5.38.xx

        try {
            context.copyXmlNode("music/settings/host", "xml/settings_prefs_compat.xml", "com.google.android.apps.youtube.music.ui.preference.PreferenceCategoryCompat")
        } catch (_: Exception) {}

        return PatchResultSuccess()
    }

}
