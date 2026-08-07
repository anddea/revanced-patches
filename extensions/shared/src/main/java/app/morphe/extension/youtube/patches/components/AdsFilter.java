package app.morphe.extension.youtube.patches.components;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.youtube.settings.Settings;

/**
 * If A/B testing is applied, ad components can only be filtered by identifier
 * <p>
 * Before A/B testing:
 * Identifier: video_display_button_group_layout.
 * Path: video_display_button_group_layout.e|ContainerType|....
 * (Path always starts with an Identifier)
 * <p>
 * After A/B testing:
 * Identifier: video_display_button_group_layout.
 * Path: video_lockup_with_attachment.e|ContainerType|....
 * (Path does not contain an Identifier)
 */
@SuppressWarnings("unused")
public final class AdsFilter extends Filter {

    private final ByteArrayFilterGroup creatorStoreShelfBuffer;
    private final StringFilterGroup creatorStoreShelf;

    public AdsFilter() {
        // Identifiers.

        final StringFilterGroup alertBannerPromo = new StringFilterGroup(
                Settings.HIDE_PROMOTION_ALERT_BANNER,
                "alert_banner_promo."
        );

        final StringFilterGroup generalAds = new StringFilterGroup(
                Settings.HIDE_GENERAL_ADS,
                "_ad_with",
                "_buttoned_layout",
                "ads_video_with_context",
                "banner_text_icon",
                "brand_video_shelf",
                "brand_video_singleton",
                "carousel_ad",
                "carousel_footered_layout",
                "carousel_headered_layout",
                "compact_landscape_image_layout", // Tablet layout search results.
                "composite_concurrent_carousel_layout",
                "full_width_portrait_image_layout",
                // full_width_square_image_button_group_layout, landscape_image_button_group_layout, text_image_button_group_layout
                "full_width_square_image_carousel_layout",
                "full_width_square_image_layout",
                "hero_promo_image",
                "image_button_group_layout",
                "landscape_image_carousel_layout",
                "landscape_image_wide_button_layout",
                "legal_disclosure",
                "lumiere_promo_carousel",
                "primetime_promo",
                "product_details",
                "shopping_timely_shelf.", // Injection point below hides the empty space.
                "square_image_layout",
                "text_image_button_layout",
                "text_image_no_button_layout", // Tablet layout search results.
                "video_display_button_group_layout",
                "video_display_carousel_button_group_layout",
                "video_display_carousel_buttoned_short_dr_layout",
                "video_display_full_buttoned_short_dr_layout",
                "video_display_full_layout",
                "watch_metadata_app_promo"
        );

        final StringFilterGroup merchandise = new StringFilterGroup(
                Settings.HIDE_MERCHANDISE_SHELF,
                "product_carousel",
                "shopping_carousel"
        );

        final StringFilterGroup promotionBanner = new StringFilterGroup(
                Settings.HIDE_YOUTUBE_PREMIUM_PROMOTION,
                "statement_banner"
        );

        final StringFilterGroup selfSponsor = new StringFilterGroup(
                Settings.HIDE_SELF_SPONSOR_CARDS,
                "cta_shelf_card"
        );

        final StringFilterGroup shoppingLinks = new StringFilterGroup(
                Settings.HIDE_SHOPPING_LINKS,
                "shopping_description_shelf"
        );

        final StringFilterGroup viewProducts = new StringFilterGroup(
                Settings.HIDE_VIEW_PRODUCTS,
                "product_item",
                "products_in_video",
                "shopping_overlay"
        );

        final StringFilterGroup webSearchPanel = new StringFilterGroup(
                Settings.HIDE_WEB_SEARCH_RESULTS,
                "web_link_panel",
                "web_result_panel"
        );

        addIdentifierCallbacks(
                alertBannerPromo,
                merchandise,
                promotionBanner,
                selfSponsor,
                shoppingLinks,
                viewProducts,
                webSearchPanel
        );

        // Path.

        creatorStoreShelf = new StringFilterGroup(
                null,
                "horizontal_shelf."
        );

        creatorStoreShelfBuffer = new ByteArrayFilterGroup(
                Settings.HIDE_CREATOR_STORE_SHELF,
                "shopping_item_card_list."
        );

        final StringFilterGroup paidContent = new StringFilterGroup(
                Settings.HIDE_PAID_PROMOTION_LABEL,
                "paid_content_overlay",
                "reel_player_disclosure",
                "shorts_disclosure"
        );

        addPathCallbacks(
                creatorStoreShelf,
                generalAds,
                paidContent,
                viewProducts
        );
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == creatorStoreShelf) {
            return contentIndex == 0 && creatorStoreShelfBuffer.check(buffer).isFiltered();
        }
        return true;
    }
}
