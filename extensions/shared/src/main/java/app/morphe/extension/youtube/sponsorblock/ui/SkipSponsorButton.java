package app.morphe.extension.youtube.sponsorblock.ui;

import static app.morphe.extension.shared.utils.ResourceUtils.getColor;
import static app.morphe.extension.shared.utils.ResourceUtils.getDimension;
import static app.morphe.extension.shared.utils.ResourceUtils.getIdIdentifier;
import static app.morphe.extension.shared.utils.ResourceUtils.getLayoutIdentifier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Objects;

import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.sponsorblock.SegmentPlaybackController;
import app.morphe.extension.youtube.sponsorblock.objects.SponsorSegment;

public class SkipSponsorButton extends FrameLayout {
    /**
     * Adds a high contrast border around the skip button.
     * <p>
     * This feature is not currently used.
     * If this is added, it needs an additional button width change because
     * as-is the skip button text is clipped when this is on.
     */
    private static final boolean highContrast = false;
    private final LinearLayout skipSponsorBtnContainer;
    private final TextView skipSponsorTextView;
    private final Paint background;
    private final Paint border;
    private SponsorSegment segment;

    public SkipSponsorButton(Context context) {
        this(context, null);
    }

    public SkipSponsorButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SkipSponsorButton(Context context, AttributeSet attributeSet, int defStyleAttr) {
        this(context, attributeSet, defStyleAttr, 0);
    }

    public SkipSponsorButton(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(getLayoutIdentifier("revanced_sb_skip_sponsor_button"), this, true);  // layout:revanced_sb_skip_sponsor_button
        setMinimumHeight(getDimension("ad_skip_ad_button_min_height"));  // dimen:ad_skip_ad_button_min_height
        skipSponsorBtnContainer = (LinearLayout) Objects.requireNonNull((View) findViewById(getIdIdentifier("revanced_sb_skip_sponsor_button_container")));  // id:revanced_sb_skip_sponsor_button_container

        background = new Paint();
        background.setColor(getColor("skip_ad_button_background_color"));  // color:skip_ad_button_background_color);
        background.setStyle(Paint.Style.FILL);

        border = new Paint();
        border.setColor(getColor("skip_ad_button_border_color"));  // color:skip_ad_button_border_color);
        border.setStrokeWidth(getDimension("ad_skip_ad_button_border_width"));  // dimen:ad_skip_ad_button_border_width);
        border.setStyle(Paint.Style.STROKE);

        skipSponsorTextView = (TextView) Objects.requireNonNull((View) findViewById(getIdIdentifier("revanced_sb_skip_sponsor_button_text")));  // id:revanced_sb_skip_sponsor_button_text;

        updateLayout();

        skipSponsorBtnContainer.setOnClickListener(v -> {
            // The view controller handles hiding this button, but hide it here as well just in case something goofs.
            setVisibility(View.GONE);
            SegmentPlaybackController.onSkipSegmentClicked(segment);
        });
    }

    @Override  // android.view.ViewGroup
    protected final void dispatchDraw(@NonNull Canvas canvas) {
        final int left = skipSponsorBtnContainer.getLeft();
        final int top = skipSponsorBtnContainer.getTop();
        final int right = left + skipSponsorBtnContainer.getWidth();
        final int bottom = top + skipSponsorBtnContainer.getHeight();

        // Determine corner radius for rounded button
        float cornerRadius = skipSponsorBtnContainer.getHeight() / 2f;

        if (Settings.SB_SQUARE_LAYOUT.get()) {
            // Square button.
            canvas.drawRect(left, top, right, bottom, background);
            if (highContrast) {
                canvas.drawLines(new float[]{
                                right, top, left, top,
                                left, top, left, bottom,
                                left, bottom, right, bottom},
                        border); // Draw square border.
            }
        } else {
            // Rounded button.
            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, background); // Draw rounded background.
            if (highContrast) {
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, border); // Draw rounded border.
            }
        }

        super.dispatchDraw(canvas);
    }

    /**
     * Update the layout of this button.
     */
    public void updateLayout() {
        if (Settings.SB_SQUARE_LAYOUT.get()) {
            // No padding for square corners.
            setPadding(0, 0, 0, 0);
        } else {
            // Apply padding for rounded corners.
            final int padding = SponsorBlockViewController.ROUNDED_LAYOUT_MARGIN;
            setPadding(padding, 0, padding, 0);
        }
    }

    /**
     *
     */
    public void updateSkipButtonText(@NonNull SponsorSegment segment) {
        this.segment = segment;
        final String newText = segment.getSkipButtonText();
        if (newText.equals(skipSponsorTextView.getText().toString())) {
            return;
        }
        skipSponsorTextView.setText(newText);
    }
}
