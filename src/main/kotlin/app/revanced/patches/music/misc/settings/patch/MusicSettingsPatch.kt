package app.revanced.patches.music.misc.settings.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.settings.AbstractSettingsResourcePatch
import app.revanced.util.resources.MusicResourceHelper.addMusicPreference
import app.revanced.util.resources.MusicResourceHelper.addMusicPreferenceCategory
import app.revanced.util.resources.MusicResourceHelper.addReVancedMusicPreference
import app.revanced.util.resources.MusicResourceHelper.sortMusicPreferenceCategory
import org.w3c.dom.Element

@Name("music-settings")
@Description("Adds settings for ReVanced to YouTube Music.")
@DependsOn([MusicIntegrationsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicSettingsPatch : AbstractSettingsResourcePatch(
    "music/settings",
    "music/settings/host",
    false
) {
    override fun execute(context: ResourceContext): PatchResult {
        super.execute(context)
        contexts = context

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

        context.addReVancedMusicPreference()

        return PatchResultSuccess()
    }
    companion object {
        private lateinit var contexts: ResourceContext

        internal fun addMusicPreference(
            category: String,
            key: String,
            defaultValue: String
        ) {
            contexts.addMusicPreferenceCategory(category)
            contexts.sortMusicPreferenceCategory()
            contexts.addMusicPreference(category, key, defaultValue)
        }
    }
}
