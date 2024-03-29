package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

@Patch(
    name = "Visual settings icons",
    description = "Adds icons to specific preferences in the settings.",
    compatiblePackages = [CompatiblePackage("com.google.android.youtube", [])],
    use = true
)
@Suppress("unused")
object VisualSettingsIconsPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        val validTitles = setOf(
            "revanced_change_player_flyout_panel_toggle",
            "revanced_disable_hdr_auto_brightness",
            "revanced_enable_bottom_player_gestures",
            "revanced_enable_old_quality_layout",
            "revanced_enable_swipe_auto_brightness",
            "revanced_enable_swipe_brightness",
            "revanced_enable_swipe_volume",
            "revanced_extended_settings_key",
            "revanced_hide_button_create_clip",
            "revanced_hide_button_download",
            "revanced_hide_button_like_dislike",
            "revanced_hide_button_remix",
            "revanced_hide_button_report",
            "revanced_hide_button_save_to_playlist",
            "revanced_hide_button_share",
            "revanced_hide_create_button",
            "revanced_hide_home_button",
            "revanced_hide_library_button",
            "revanced_hide_notifications_button",
            "revanced_hide_player_flyout_panel_ambient_mode",
            "revanced_hide_player_flyout_panel_audio_track",
            "revanced_hide_player_flyout_panel_captions",
            "revanced_hide_player_flyout_panel_help",
            "revanced_hide_player_flyout_panel_lock_screen",
            "revanced_hide_player_flyout_panel_loop_video",
            "revanced_hide_player_flyout_panel_more_info",
            "revanced_hide_player_flyout_panel_playback_speed",
            "revanced_hide_player_flyout_panel_report",
            "revanced_hide_player_flyout_panel_stable_volume",
            "revanced_hide_player_flyout_panel_stats_for_nerds",
            "revanced_hide_player_flyout_panel_watch_in_vr",
            "revanced_hide_quick_actions_comment",
            "revanced_hide_quick_actions_dislike",
            "revanced_hide_quick_actions_like",
            "revanced_hide_quick_actions_more",
            "revanced_hide_quick_actions_save_to_playlist",
            "revanced_hide_quick_actions_share",
            "revanced_hide_shorts_button",
            "revanced_hide_shorts_player_comments_button",
            "revanced_hide_shorts_player_dislike_button",
            "revanced_hide_shorts_player_like_button",
            "revanced_hide_shorts_player_remix_button",
            "revanced_hide_shorts_player_share_button",
            "revanced_hide_shorts_toolbar_camera_button",
            "revanced_hide_shorts_toolbar_menu_button",
            "revanced_hide_shorts_toolbar_search_button",
            "revanced_hide_subscriptions_button",
            "revanced_overlay_button_always_repeat",
            "revanced_overlay_button_copy_video_url",
            "revanced_overlay_button_copy_video_url_timestamp",
            "revanced_overlay_button_external_downloader",
            "revanced_overlay_button_speed_dialog",
            "revanced_overlay_button_time_ordered_playlist",
            "revanced_overlay_button_whitelisting",
            "revanced_ryd_settings_key",
            "revanced_sponsorblock_settings_key",
            "revanced_switching_create_notification",
            "sb_enable_create_segment",
            "sb_enable_voting",
        )

        val emptyTitles = setOf(
            "external_downloader",
            "revanced_hide_player_flyout_panel_captions_footer",
            "revanced_hide_player_flyout_panel_quality_footer",
            "whitelisting",
        )

        arrayOf(
            ResourceGroup(
                "drawable-xxhdpi",
                *validTitles.map { it + "_icon.png" }.toTypedArray(),
                "empty_icon.png"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/settings", resourceGroup)
        }

        val tagNames = listOf("SwitchPreference", "PreferenceScreen", "Preference")

        @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
        fun Document.getElementsByTagName(tagName: String): NodeList {
            return this.getElementsByTagName(tagName)
        }

        fun processPreferences(file: Document) {
            tagNames.forEach { tagName ->
                val elements = file.getElementsByTagName(tagName)
                for (i in 0 until elements.length) {
                    val preference = elements.item(i) as? Element
                    when (val title = preference?.getAttribute("android:key")) {
                        in validTitles -> preference?.setAttribute("android:icon", "@drawable/${title}_icon")
                        in emptyTitles -> preference?.setAttribute("android:icon", "@drawable/empty_icon")
                    }
                }
            }
        }

        context.xmlEditor["res/xml/revanced_prefs.xml"].use { editor ->
            processPreferences(editor.file)
        }

        context.xmlEditor["res/xml/settings_fragment.xml"].use { editor ->
            processPreferences(editor.file)
        }

        SettingsPatch.updatePatchStatus("Visual settings icons")

    }
}
