package app.revanced.patches.youtube.layout.optimize.patch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class RedundantResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        // Convert universal APK to anti-split APK
        arrayOf(
            WHITELIST_MDPI,
            WHITELIST_HDPI,
            WHITELIST_XHDPI,
            WHITELIST_XXXHDPI
        ).forEach { (path, array) ->
            val tmpDirectory = "$path-v21"
            Files.createDirectory(context["res"].resolve(tmpDirectory).toPath())

            (WHITELIST_GENERAL + array).forEach { name ->
                try {
                    Files.copy(
                        context["res"].resolve("$path/$name").toPath(),
                        context["res"].resolve("$tmpDirectory/$name").toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (_: Exception) {
                }
            }
            val directoryPath = context["res"].resolve(path)

            Files.walk(directoryPath.toPath())
                .map(Path::toFile)
                .sorted(Comparator.reverseOrder())
                .forEach(File::delete)

            Files.move(
                context["res"].resolve(tmpDirectory).toPath(),
                context["res"].resolve(path).toPath()
            )
        }

        context.copyResources(
            "youtube/resource",
            ResourceUtils.ResourceGroup(
                "raw",
                "third_party_licenses"
            )
        )

    }

    private companion object {
        val WHITELIST_GENERAL = arrayOf(
            "product_logo_youtube_color_24.png",
            "product_logo_youtube_color_36.png",
            "product_logo_youtube_color_144.png",
            "product_logo_youtube_color_192.png",
            "yt_outline_audio_black_24.png",
            "yt_outline_bag_black_24.png",
            "yt_outline_fashion_black_24.png",
            "yt_outline_film_strip_black_24.png",
            "yt_outline_fire_black_24.png",
            "yt_outline_gaming_black_24.png",
            "yt_outline_lightbulb_black_24.png",
            "yt_outline_news_black_24.png",
            "yt_outline_radar_live_black_24.png",
            "yt_outline_trophy_black_24.png",
            "yt_premium_wordmark_header_dark.png",
            "yt_premium_wordmark_header_light.png"
        )

        val WHITELIST_MDPI = "drawable-mdpi" to arrayOf(
            "ic_searchable.webp",
            "generic_dark_x1.png",
            "generic_light_x1.png"
        )

        val WHITELIST_HDPI = "drawable-hdpi" to arrayOf(
            "transition.png"
        )

        val WHITELIST_XHDPI = "drawable-xhdpi" to arrayOf(
            "action_bar_logo_release.png",
            "ad_feed_call_to_action_arrow.webp",
            "ad_skip.png",
            "alert_error.png",
            "api_btn_cc_off.png",
            "api_btn_cc_on.png",
            "api_btn_hd_off.png",
            "api_btn_hd_on.png",
            "api_btn_hq_off.png",
            "api_btn_hq_on.png",
            "api_btn_next.png",
            "api_btn_pause.png",
            "api_btn_play.png",
            "api_btn_prev.png",
            "api_btn_replay.png",
            "api_btn_unavailable.png",
            "api_ic_full_screen.png",
            "api_ic_full_screen_selected.png",
            "api_ic_live.9.png",
            "api_ic_options.png",
            "api_ic_options_selected.png",
            "api_ic_small_screen.png",
            "api_ic_small_screen_selected.png",
            "api_player_bar.9.png",
            "api_player_buffered.9.png",
            "api_player_menu_bar.9.png",
            "api_player_track.9.png",
            "api_play_on_you_tube.png",
            "api_scrubber.png",
            "api_scrubber_selected.png",
            "box_shadow.9.png",
            "btn_play_all.9.png",
            "card_frame_bottom.9.png",
            "card_frame_middle.9.png",
            "circle_shadow.9.png",
            "common_full_open_on_phone.png",
            "compat_selector_disabled.9.png",
            "compat_selector_focused.9.png",
            "compat_selector_longpressed.9.png",
            "compat_selector_pressed.9.png",
            "ic_account_switcher_alert.png",
            "ic_annotation_close.png",
            "ic_api_youtube_logo.png",
            "ic_api_youtube_logo_pressed.png",
            "ic_api_youtube_watermark.png",
            "ic_notification_error.png",
            "ic_notification_error_small.webp",
            "ic_playlist_icon.png",
            "ic_unavailable_common.webp",
            "ic_youtube_logo.png",
            "lc_editbox_dropdown_background_dark.9.png",
            "star_empty.webp",
            "star_filled.webp",
            "survey_checked.png",
            "survey_unchecked.png",
            "textfield_default_mtrl_alpha.9.png",
            "youtube_lozenge_logo.png",
            "youtube_lozenge_logo_dni.png"
        )

        val WHITELIST_XXXHDPI = "drawable-xxxhdpi" to arrayOf(
            "ic_group_collapse_00.png",
            "ic_group_collapse_01.png",
            "ic_group_collapse_02.png",
            "ic_group_collapse_03.png",
            "ic_group_collapse_04.png",
            "ic_group_collapse_05.png",
            "ic_group_collapse_06.png",
            "ic_group_collapse_07.png",
            "ic_group_collapse_08.png",
            "ic_group_collapse_09.png",
            "ic_group_collapse_10.png",
            "ic_group_collapse_11.png",
            "ic_group_collapse_12.png",
            "ic_group_collapse_13.png",
            "ic_group_collapse_14.png",
            "ic_group_collapse_15.png",
            "ic_group_expand_00.png",
            "ic_group_expand_01.png",
            "ic_group_expand_02.png",
            "ic_group_expand_03.png",
            "ic_group_expand_04.png",
            "ic_group_expand_05.png",
            "ic_group_expand_06.png",
            "ic_group_expand_07.png",
            "ic_group_expand_08.png",
            "ic_group_expand_09.png",
            "ic_group_expand_10.png",
            "ic_group_expand_11.png",
            "ic_group_expand_12.png",
            "ic_group_expand_13.png",
            "ic_group_expand_14.png",
            "ic_group_expand_15.png"
        )
    }
}
