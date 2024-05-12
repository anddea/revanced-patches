package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.layout.branding.icon.CustomBrandingIconPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

@Suppress("DEPRECATION", "unused")
object VisualPreferencesIconsPatch : BaseResourcePatch(
    name = "Visual preferences icons",
    description = "Adds icons to specific preferences in the settings.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private val MainSettings by booleanPatchOption(
        key = "MainSettings",
        default = true,
        title = "Main settings",
        description = "Apply icons to main settings",
        required = true
    )

    private val ExtendedSettings by booleanPatchOption(
        key = "ExtendedSettings",
        default = true,
        title = "Extended settings",
        description = "Apply icons to Extended main screen settings",
        required = true
    )

    private const val DEFAULT_ICON_KEY = "Extension"

    private val ExtendedIcon by stringPatchOption(
        key = "ExtendedIcon",
        default = DEFAULT_ICON_KEY,
        values = mapOf(
            "Custom branding icon" to "custom_branding_icon",
            DEFAULT_ICON_KEY to "extension",
            "Gear" to "gear",
            "ReVanced" to "revanced",
            "ReVanced Colored" to "revanced_colored",
        ),
        title = "Extended icon",
        description = "Apply different icons for Extended preference."
    )

    override fun execute(context: ResourceContext) {

        val validTitles = setOf(
            "revanced_change_player_flyout_panel_toggle",
            "revanced_default_playback_speed",
            "revanced_default_video_quality_wifi",
            "revanced_disable_hdr_auto_brightness",
            "revanced_disable_hdr_video",
            "revanced_enable_bottom_player_gestures",
            "revanced_enable_default_playback_speed_shorts",
            "revanced_enable_old_quality_layout",
            "revanced_enable_swipe_lowest_value_auto_brightness",
            "revanced_enable_swipe_brightness",
            "revanced_enable_swipe_volume",
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
            "revanced_hide_quick_actions_comment_button",
            "revanced_hide_quick_actions_dislike_button",
            "revanced_hide_quick_actions_like_button",
            "revanced_hide_quick_actions_live_chat_button",
            "revanced_hide_quick_actions_more_button",
            "revanced_hide_quick_actions_save_to_playlist_button",
            "revanced_hide_quick_actions_share_button",
            "revanced_hide_shorts_button",
            "revanced_hide_shorts_comments_button",
            "revanced_hide_shorts_dislike_button",
            "revanced_hide_shorts_like_button",
            "revanced_hide_shorts_remix_button",
            "revanced_hide_shorts_share_button",
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
            "revanced_overlay_button_whitelist",
            "revanced_switching_create_notification",
            "sb_enable_create_segment",
            "sb_enable_voting",

            "revanced_preference_screen_player_buttons",
            "revanced_preference_screen_action_buttons",
            "revanced_preference_screen_comments",
            "revanced_preference_screen_player_flyout_menu",
            "revanced_preference_screen_fullscreen",
            "revanced_preference_screen_navigation_buttons",
            "revanced_preference_screen_seekbar",
        )

        val validMainTitles = setOf(
            "parent_tools_key",
            "general_key",
            "account_switcher_key",
            "data_saving_settings_key",
            "auto_play_key",
            "video_quality_settings_key",
            "offline_key",
            "pair_with_tv_key",
            "history_key",
            "your_data_key",
            "privacy_key",
            "premium_early_access_browse_page_key",
            "subscription_product_setting_key",
            "billing_and_payment_key",
            "notification_key",
            "connected_accounts_browse_page_key",
            "live_chat_key",
            "captions_key",
            "accessibility_settings_key",
            "about_key",
        )

        val validExtendedTitles = setOf(
            "revanced_preference_screen_general",
            "revanced_preference_screen_ads",
            "revanced_preference_screen_alt_thumbnails",
            "revanced_preference_screen_player",
            "revanced_preference_screen_shorts",
            "revanced_preference_screen_swipe_controls",
            "revanced_preference_screen_video",
            "revanced_preference_screen_ryd",
            "revanced_preference_screen_sb",
            "revanced_preference_screen_misc",
        )

        val validExtendedBrand = setOf(
            "revanced_extended_settings_key",
        )

        val emptyTitles = setOf(
            "revanced_custom_playback_speeds",
            "revanced_custom_playback_speed_menu_type",
            "revanced_default_video_quality_mobile",
            "revanced_disable_default_playback_speed_live",
            "external_downloader",
            "revanced_enable_custom_playback_speed",
            "revanced_hide_shorts_comments_disabled_button",
            "revanced_hide_player_flyout_panel_captions_footer",
            "revanced_hide_player_flyout_panel_quality_footer",
            "revanced_remember_playback_speed_last_selected",
            "revanced_remember_video_quality_last_selected",
            "revanced_restore_old_video_quality_menu",
        )

        // A lot of mappings here.
        // The performance impact should be negligible in this context,
        // as the operations involved are not computationally intensive.
        val validTitlesIcons = validTitles.associateWith { title ->
            when (title) {
                "revanced_disable_hdr_auto_brightness" -> "revanced_disable_hdr_video_icon"
                "revanced_hide_shorts_comments_button" -> "revanced_hide_quick_actions_comment_button_icon"
                "revanced_enable_bottom_player_gestures" -> "revanced_preference_screen_swipe_controls_icon"
                "revanced_hide_shorts_button" -> "revanced_preference_screen_shorts_icon"
                "revanced_hide_button_like_dislike" -> "sb_enable_voting_icon"
                "revanced_hide_shorts_like_button" -> "revanced_hide_quick_actions_like_button_icon"
                "revanced_hide_shorts_dislike_button" -> "revanced_preference_screen_ryd_icon"
                "revanced_hide_quick_actions_dislike_button" -> "revanced_preference_screen_ryd_icon"
                "revanced_hide_quick_actions_share_button" -> "revanced_hide_shorts_share_button_icon"
                "revanced_default_playback_speed" -> "revanced_overlay_button_speed_dialog_icon"
                "revanced_enable_old_quality_layout" -> "revanced_default_video_quality_wifi_icon"
                "revanced_hide_button_download" -> "revanced_overlay_button_external_downloader_icon"
                "revanced_hide_button_share" -> "revanced_hide_shorts_share_button_icon"
                "revanced_hide_library_button" -> "revanced_preference_screen_video_icon"
                "revanced_hide_notifications_button" -> "notification_key_icon"
                "revanced_hide_quick_actions_save_to_playlist_button" -> "revanced_hide_button_save_to_playlist_icon"
                "revanced_hide_player_flyout_panel_report" -> "revanced_hide_button_report_icon"
                "revanced_hide_player_flyout_panel_more_info" -> "about_key_icon"
                "revanced_hide_player_flyout_panel_captions" -> "captions_key_icon"
                "revanced_hide_player_flyout_panel_loop_video" -> "revanced_overlay_button_always_repeat_icon"
                "revanced_hide_button_remix" -> "revanced_hide_shorts_remix_button_icon"
                "revanced_preference_screen_comments" -> "revanced_hide_quick_actions_comment_button_icon"
                else -> "${title}_icon"
            }
        }

        val validMainTitlesIcons = validMainTitles.associateWith { "${it}_icon" }
        val validExtendedBrandIcon = validExtendedBrand.associateWith { "${it}_icon" }

        val validExtendedTitlesIcons = validExtendedTitles.associateWith { title ->
            when (title) {
                "revanced_preference_screen_general" -> "general_key_icon"
                "revanced_preference_screen_sb" -> "sb_enable_create_segment_icon"
                else -> "${title}_icon"
            }
        }

        // Copy resources
        val emptyIcon = "empty_icon"
        val resourcesToCopy = mutableListOf(
            ResourceGroup("drawable-xxhdpi", "$emptyIcon.png"),
            ResourceGroup("drawable", *validTitlesIcons.values.map { "$it.xml" }.toTypedArray())
        )

        fun copyResourcesWithFallback(iconPath: String) {
            try {
                context.copyResources(iconPath, ResourceGroup("drawable", "revanced_extended_settings_key_icon.xml"))
            } catch (_: Exception) {
                // Ignore if resource copy fails

                // Add a fallback extended icon
                // It's needed if someone provides custom path to icon(s) folder
                // but custom branding icons for Extended setting are predefined,
                // so it won't copy custom branding icon
                // and will raise an error without fallback icon
                context.copyResources("youtube/settings/icons/extension", ResourceGroup("drawable", "revanced_extended_settings_key_icon.xml"))
            }
        }

        MainSettings?.let {
            resourcesToCopy.add(ResourceGroup("drawable", *validMainTitlesIcons.values.map { "$it.xml" }.toTypedArray()))
            ExtendedIcon?.let { iconType ->
                val selectedIconType = iconType.lowercase().replace(" ", "_")
                CustomBrandingIconPatch.AppIcon?.let { appIcon ->
                    val appIconValue = appIcon.lowercase().replace(" ", "_")
                    val resourcePath = "youtube/branding/$appIconValue"

                    val iconPath = when {
                        selectedIconType == "custom_branding_icon" -> "$resourcePath/launcher"
                        else -> "youtube/settings/icons/$selectedIconType"
                    }

                    copyResourcesWithFallback(iconPath)
                }
            }
        }

        ExtendedSettings?.let {
            resourcesToCopy.add(ResourceGroup("drawable", *validExtendedTitlesIcons.values.map { "$it.xml" }.toTypedArray()))
        }

        resourcesToCopy.forEach { context.copyResources("youtube/settings/icons", it) }

        // Edit Preferences / add icon attribute
        val tagNames = listOf(
            "app.revanced.integrations.shared.settings.preference.ResettableEditTextPreference",
            "app.revanced.integrations.youtube.settings.preference.ExternalDownloaderPreference",
            "ListPreference",
            "Preference",
            "PreferenceScreen",
            "SwitchPreference",
        )

        @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
        fun Document.getElementsByTagName(tagName: String): NodeList {
            return this.getElementsByTagName(tagName)
        }

        // Attach icons to prefs
        fun processPreferences(file: Document) {
            tagNames.forEach { tagName ->
                val elements = file.getElementsByTagName(tagName)
                for (i in 0 until elements.length) {
                    val preference = elements.item(i) as? Element
                    val title = preference?.getAttribute("android:key")?.removePrefix("@string/")
                    val icon = when {
                        title in validTitles -> validTitlesIcons[title]
                        MainSettings!! && title in validMainTitles -> validMainTitlesIcons[title]
                        ExtendedSettings!! && title in validExtendedTitles -> validExtendedTitlesIcons[title]

                        // Add custom extended icon (only if main settings icons applied)
                        MainSettings!! && title in validExtendedBrand -> validExtendedBrandIcon[title]
                        title in emptyTitles -> emptyIcon
                        else -> null
                    }
                    icon?.let { preference?.setAttribute("android:icon", "@drawable/$it") }
                }
            }
        }

        context.xmlEditor["res/xml/revanced_prefs.xml"].use { editor ->
            processPreferences(editor.file)
        }

        context.xmlEditor["res/xml/settings_fragment.xml"].use { editor ->
            processPreferences(editor.file)
        }

        SettingsPatch.updatePatchStatus("Visual preferences icons")
    }
}
