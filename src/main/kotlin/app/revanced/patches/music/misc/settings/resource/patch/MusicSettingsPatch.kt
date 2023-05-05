package app.revanced.patches.music.misc.settings.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.settings.bytecode.patch.MusicSettingsBytecodePatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.settings.AbstractSettingsResourcePatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.enum.CategoryType.*
import app.revanced.util.resources.MusicResourceHelper.addMusicPreference
import app.revanced.util.resources.MusicResourceHelper.addMusicPreferenceAlt
import app.revanced.util.resources.MusicResourceHelper.addMusicPreferenceCategory
import app.revanced.util.resources.MusicResourceHelper.addMusicPreferenceWithIntent
import app.revanced.util.resources.MusicResourceHelper.addReVancedMusicPreference
import app.revanced.util.resources.MusicResourceHelper.sortMusicPreferenceCategory
import org.w3c.dom.Element

@Patch
@Name("music-settings")
@Description("Adds settings for ReVanced to YouTube Music.")
@DependsOn([MusicSettingsBytecodePatch::class])
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

        /**
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
            category: CategoryType,
            key: String,
            defaultValue: String
        ) {
            val categoryValue = category.value
            contexts.addMusicPreferenceCategory(categoryValue)
            contexts.addMusicPreference(categoryValue, key, defaultValue)
            contexts.sortMusicPreferenceCategory(categoryValue)
        }

        internal fun addMusicPreferenceAlt(
            category: CategoryType,
            key: String,
            defaultValue: String,
            dependencyKey: String

        ) {
            val categoryValue = category.value
            contexts.addMusicPreferenceCategory(categoryValue)
            contexts.addMusicPreferenceAlt(categoryValue, key, defaultValue, dependencyKey)
            contexts.sortMusicPreferenceCategory(categoryValue)
        }

        internal fun addMusicPreferenceWithIntent(
            category: CategoryType,
            key: String,
            dependencyKey: String

        ) {
            val categoryValue = category.value
            contexts.addMusicPreferenceCategory(categoryValue)
            contexts.addMusicPreferenceWithIntent(categoryValue, key, dependencyKey)
            contexts.sortMusicPreferenceCategory(categoryValue)
        }
    }
}
