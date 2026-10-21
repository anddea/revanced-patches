/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 * Written by lavinhoque33, 2026-10-10.
 */

package app.morphe.extension.youtube.patches.general.offline;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import app.morphe.extension.shared.ui.Dim;
import app.morphe.extension.shared.ui.SheetBottomDialog;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.shared.PipDismissHelper;
import app.morphe.extension.youtube.utils.ThemeUtils;

/**
 * Bottom sheet of the built-in downloader. Shows the video while its formats load,
 * then one row per downloadable quality.
 */
final class OfflinePickerSheet {
    private static final String THUMBNAIL_URL = "https://i.ytimg.com/vi/%s/mqdefault.jpg";
    private static final int ANIMATION_DURATION_MS = 180;
    private static final float MAX_LIST_HEIGHT_FRACTION = 0.55f;

    private final Activity activity;
    private final SheetBottomDialog.SlideDialog dialog;
    private final LinearLayout root;
    private final ImageView thumbnail;
    private final TextView duration;
    private final TextView title;
    private final TextView author;
    private final ProgressBar loading;

    private final int foreground;
    private final int secondary;
    private final int divider;
    private final int ripple;

    OfflinePickerSheet(Activity activity, String videoId, Runnable onDismiss) {
        this.activity = activity;
        foreground = ThemeUtils.getAppForegroundColor();
        secondary = withAlpha(foreground, 0xAA);
        divider = withAlpha(foreground, 0x1F);
        ripple = withAlpha(foreground, 0x33);

        root = SheetBottomDialog.createMainLayout(activity, ThemeUtils.getDialogBackgroundColor());
        root.setPadding(0, 0, 0, Dim.dp8);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Dim.dp16, Dim.dp4, Dim.dp16, Dim.dp12);

        FrameLayout thumbnailFrame = new FrameLayout(activity);
        thumbnail = new ImageView(activity);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setClipToOutline(true);
        thumbnail.setBackground(rounded(Dim.dp8, divider));
        thumbnailFrame.addView(thumbnail, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        duration = text(11, Color.WHITE, 1);
        duration.setTypeface(Typeface.DEFAULT_BOLD);
        duration.setBackground(rounded(Dim.dp4, 0xCC000000));
        duration.setPadding(Dim.dp4, 0, Dim.dp4, 0);
        duration.setVisibility(View.GONE);
        FrameLayout.LayoutParams durationParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
        durationParams.setMargins(Dim.dp4, Dim.dp4, Dim.dp4, Dim.dp4);
        thumbnailFrame.addView(duration, durationParams);
        header.addView(thumbnailFrame, new LinearLayout.LayoutParams(Dim.dp(128), Dim.dp(72)));

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(Dim.dp12, 0, 0, 0);
        title = text(16, foreground, 2);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setText(str("revanced_offline_loading_formats"));
        texts.addView(title);
        author = text(13, secondary, 1);
        author.setVisibility(View.GONE);
        texts.addView(author);
        header.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        loading = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        loading.setIndeterminate(true);
        loading.setIndeterminateTintList(ColorStateList.valueOf(foreground));
        LinearLayout.LayoutParams loadingParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        loadingParams.setMargins(Dim.dp16, 0, Dim.dp16, Dim.dp16);
        root.addView(loading, loadingParams);

        dialog = SheetBottomDialog.createSlideDialog(activity, root, ANIMATION_DURATION_MS);
        dialog.setOnDismissListener(d -> onDismiss.run());
        PipDismissHelper.dismissOnPip(dialog);
        loadThumbnail(videoId);
    }

    void show() {
        dialog.show();
    }

    void dismiss() {
        try {
            dialog.dismiss();
        } catch (Exception ex) {
            Logger.printDebug(() -> "Could not dismiss the quality picker", ex);
        }
    }

    /**
     * Replaces the loading state with one row per quality.
     */
    void showFormats(OfflineFormats.Resolved resolved, Consumer<OfflineFormats.Stream> onPick) {
        title.setText(resolved.title());
        author.setText(resolved.author());
        author.setVisibility(TextUtils.isEmpty(resolved.author()) ? View.GONE : View.VISIBLE);
        if (resolved.lengthMs() > 0) {
            duration.setText(OfflineLibraryScreen.formatDuration(resolved.lengthMs()));
            duration.setVisibility(View.VISIBLE);
        }
        root.removeView(loading);

        View line = new View(activity);
        line.setBackgroundColor(divider);
        root.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Dim.dp1));

        TextView section = text(13, secondary, 1);
        section.setTypeface(Typeface.DEFAULT_BOLD);
        section.setText(str("revanced_offline_picker_subtitle"));
        section.setPadding(Dim.dp16, Dim.dp12, Dim.dp16, Dim.dp4);
        root.addView(section);

        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        List<OfflineFormats.Stream> videos = resolved.videos();
        boolean[] picked = {false};
        for (OfflineFormats.Stream video : videos) {
            list.addView(row(video, resolved.audioFor(video).contentLength(), v -> {
                if (picked[0]) return; // A second tap while the sheet slides away.
                picked[0] = true;
                onPick.accept(video);
            }));
        }
        ScrollView scroll = new MaxHeightScrollView(activity,
                (int) (activity.getResources().getDisplayMetrics().heightPixels * MAX_LIST_HEIGHT_FRACTION));
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView footer = text(12, secondary, 1);
        footer.setText(str("revanced_offline_picker_footer"));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(Dim.dp16, Dim.dp8, Dim.dp16, Dim.dp4);
        root.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private View row(OfflineFormats.Stream video, long audioLength, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Dim.dp(60));
        row.setPadding(Dim.dp16, Dim.dp8, Dim.dp16, Dim.dp8);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(ripple), null, mask));
        row.setOnClickListener(onClick);

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);

        LinearLayout qualityLine = new LinearLayout(activity);
        qualityLine.setOrientation(LinearLayout.HORIZONTAL);
        qualityLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView quality = text(16, foreground, 1);
        quality.setTypeface(Typeface.DEFAULT_BOLD);
        quality.setText(OfflineDownloader.qualityLabel(video));
        qualityLine.addView(quality);
        String badgeText = video.height() >= 2160 ? "4K" : video.height() >= 1440 ? "2K" : video.height() >= 720 ? "HD" : null;
        if (badgeText != null) {
            TextView badge = text(10, foreground, 1);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setText(badgeText);
            badge.setBackground(rounded(Dim.dp4, divider));
            badge.setPadding(Dim.dp6, Dim.dp1, Dim.dp6, Dim.dp1);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeParams.setMarginStart(Dim.dp8);
            qualityLine.addView(badge, badgeParams);
        }
        texts.addView(qualityLine);

        TextView codec = text(13, secondary, 1);
        boolean av1 = video.codec().toUpperCase(Locale.ROOT).startsWith("AV1");
        boolean vp9 = OfflineFormats.CODEC_VP9.equals(video.codec());
        // AV1 this device cannot decode is listed, with a warning note.
        SpannableStringBuilder codecText = new SpannableStringBuilder(video.codec() + " · "
                + str(av1 ? "revanced_offline_picker_av1_hint"
                : vp9 ? "revanced_offline_picker_vp9_hint" : "revanced_offline_picker_h264_hint"));
        if (!video.playable()) {
            int start = codecText.length();
            codecText.append(" [").append(str("revanced_offline_picker_av1_unplayable")).append(']');
            codecText.setSpan(new ForegroundColorSpan(
                            BaseThemeUtils.isDarkModeEnabled() ? 0xFFFFA040 : 0xFFD84315),
                    start, codecText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            codec.setMaxLines(2);
        }
        codec.setText(codecText);
        texts.addView(codec);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView size = text(14, foreground, 1);
        size.setText(video.contentLength() > 0 && audioLength > 0
                ? Formatter.formatShortFileSize(activity, video.contentLength() + audioLength)
                : "—");
        size.setPadding(Dim.dp12, 0, 0, 0);
        row.addView(size);
        return row;
    }

    private void loadThumbnail(String videoId) {
        Utils.runOnBackgroundThread(() -> {
            Bitmap bitmap = fetchThumbnail(String.format(Locale.US, THUMBNAIL_URL, videoId));
            if (bitmap != null) Utils.runOnMainThread(() -> thumbnail.setImageBitmap(bitmap));
        });
    }

    @Nullable
    private static Bitmap fetchThumbnail(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            try (InputStream input = connection.getInputStream()) {
                return BitmapFactory.decodeStream(input);
            }
        } catch (Exception ex) {
            Logger.printDebug(() -> "Could not load the picker thumbnail", ex);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private TextView text(float sizeSp, int color, int maxLines) {
        TextView view = new TextView(activity);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setMaxLines(maxLines);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }

    private static GradientDrawable rounded(int radius, int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radius);
        shape.setColor(color);
        return shape;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /** Keeps the sheet's header and footer on screen when a video has many qualities. */
    private static final class MaxHeightScrollView extends ScrollView {
        private final int maxHeight;

        MaxHeightScrollView(Context context, int maxHeight) {
            super(context);
            this.maxHeight = maxHeight;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
        }
    }
}
