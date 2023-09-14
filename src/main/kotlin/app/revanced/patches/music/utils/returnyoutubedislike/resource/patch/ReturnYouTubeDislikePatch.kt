package app.revanced.patches.music.utils.returnyoutubedislike.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.returnyoutubedislike.bytecode.patch.ReturnYouTubeDislikeBytecodePatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.MusicResourceHelper.RETURN_YOUTUBE_DISLIKE_SETTINGS_KEY
import app.revanced.util.resources.MusicResourceHelper.addReVancedMusicPreference
import org.w3c.dom.Element
import org.w3c.dom.Node

@Patch
@Name("Return YouTube Dislike")
@Description("Shows the dislike count of videos using the Return YouTube Dislike API.")
@DependsOn(
    [
        ReturnYouTubeDislikeBytecodePatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
class ReturnYouTubeDislikePatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        context.addReVancedMusicPreference(RETURN_YOUTUBE_DISLIKE_SETTINGS_KEY)
        context.addSwitchPreference("revanced_ryd_enabled", "true")
        context.addSwitchPreference(
            "revanced_ryd_dislike_percentage",
            "false",
            "revanced_ryd_enabled"
        )
        context.addSwitchPreference("revanced_ryd_compact_layout", "false", "revanced_ryd_enabled")
        context.addPreferenceCategory("revanced_ryd_about")
        context.addAboutPreference("revanced_ryd_attribution")

    }

    private companion object {
        const val YOUTUBE_MUSIC_SETTINGS_PATH = "res/xml/settings_headers.xml"
        const val SWITCH_PREFERENCE_TAG_NAME =
            "com.google.android.apps.youtube.music.ui.preference.SwitchCompatPreference"
        const val PREFERENCE_CATEGORY_TAG_NAME =
            "com.google.android.apps.youtube.music.ui.preference.PreferenceCategoryCompat"

        private fun Node.adoptChild(tagName: String, block: Element.() -> Unit) {
            val child = ownerDocument.createElement(tagName)
            child.block()
            appendChild(child)
        }

        fun ResourceContext.addAboutPreference(
            key: String
        ) {
            this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
                val tags = editor.file.getElementsByTagName(PREFERENCE_CATEGORY_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter { it.getAttribute("android:key").contains("revanced_ryd_about") }
                    .forEach {
                        it.adoptChild("Preference") {
                            setAttribute("android:title", "@string/$key" + "_title")
                            setAttribute("android:summary", "@string/$key" + "_summary")
                            setAttribute("android:key", key)
                            this.adoptChild("intent") {
                                setAttribute("android:action", "android.intent.action.VIEW")
                                setAttribute("android:data", "https://returnyoutubedislike.com")
                            }
                        }
                    }
            }
        }

        fun ResourceContext.addPreferenceCategory(
            category: String
        ) {
            this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
                val tags = editor.file.getElementsByTagName("PreferenceScreen")
                List(tags.length) { tags.item(it) as Element }
                    .filter {
                        it.getAttribute("android:key").contains(RETURN_YOUTUBE_DISLIKE_SETTINGS_KEY)
                    }
                    .forEach {
                        it.adoptChild(PREFERENCE_CATEGORY_TAG_NAME) {
                            setAttribute("android:title", "@string/$category")
                            setAttribute("android:key", category)
                        }
                    }
            }
        }

        fun ResourceContext.addSwitchPreference(
            key: String,
            defaultValue: String
        ) {
            addSwitchPreference(key, defaultValue, "")
        }

        fun ResourceContext.addSwitchPreference(
            key: String,
            defaultValue: String,
            dependencyKey: String
        ) {
            this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
                val tags = editor.file.getElementsByTagName("PreferenceScreen")
                List(tags.length) { tags.item(it) as Element }
                    .filter {
                        it.getAttribute("android:key").contains(RETURN_YOUTUBE_DISLIKE_SETTINGS_KEY)
                    }
                    .forEach {
                        it.adoptChild(SWITCH_PREFERENCE_TAG_NAME) {
                            setAttribute("android:title", "@string/$key" + "_title")
                            setAttribute("android:summary", "@string/$key" + "_summary")
                            setAttribute("android:key", key)
                            if (dependencyKey.isNotEmpty()) {
                                setAttribute("android:dependency", dependencyKey)
                            }
                            setAttribute("android:defaultValue", defaultValue)
                        }
                    }
            }
        }
    }
}