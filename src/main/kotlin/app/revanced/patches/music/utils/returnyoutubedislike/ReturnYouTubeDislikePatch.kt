package app.revanced.patches.music.utils.returnyoutubedislike

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.PREFERENCE_CATEGORY_TAG_NAME
import app.revanced.patches.music.utils.settings.ResourceUtils.SETTINGS_HEADER_PATH
import app.revanced.patches.music.utils.settings.ResourceUtils.addPreferenceCategoryUnderPreferenceScreen
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.adoptChild
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element

@Suppress("DEPRECATION", "unused")
object ReturnYouTubeDislikePatch : BaseResourcePatch(
    name = "Return YouTube Dislike",
    description = "Adds an option to show the dislike count of songs using the Return YouTube Dislike API.",
    dependencies = setOf(
        ReturnYouTubeDislikeBytecodePatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val ABOUT_CATEGORY_KEY = "revanced_ryd_about"

    override fun execute(context: ResourceContext) {

        SettingsPatch.addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "revanced_ryd_enabled",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "revanced_ryd_dislike_percentage",
            "false",
            "revanced_ryd_enabled"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "revanced_ryd_compact_layout",
            "false",
            "revanced_ryd_enabled"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "revanced_ryd_toast_on_connection_error",
            "false",
            "revanced_ryd_enabled"
        )

        context.addPreferenceCategoryUnderPreferenceScreen(
            CategoryType.RETURN_YOUTUBE_DISLIKE.value,
            ABOUT_CATEGORY_KEY
        )

        context.addAboutPreference(
            ABOUT_CATEGORY_KEY,
            "revanced_ryd_attribution",
            "https://returnyoutubedislike.com"
        )

    }

    private fun ResourceContext.addAboutPreference(
        preferenceCategoryKey: String,
        key: String,
        data: String
    ) {
        this.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_CATEGORY_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(preferenceCategoryKey) }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/$key" + "_title")
                        setAttribute("android:summary", "@string/$key" + "_summary")
                        setAttribute("android:key", key)
                        this.adoptChild("intent") {
                            setAttribute("android:action", "android.intent.action.VIEW")
                            setAttribute("android:data", data)
                        }
                    }
                }
        }
    }

}