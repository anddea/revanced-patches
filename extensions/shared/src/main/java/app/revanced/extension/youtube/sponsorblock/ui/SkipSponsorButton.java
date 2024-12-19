package app.revanced.extension.youtube.sponsorblock.ui;

import static app.revanced.extension.shared.utils.ResourceUtils.getDimension;
import static app.revanced.extension.shared.utils.ResourceUtils.getIdIdentifier;
import static app.revanced.extension.shared.utils.ResourceUtils.getLayoutIdentifier;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Objects;

import app.revanced.extension.youtube.sponsorblock.SegmentPlaybackController;
import app.revanced.extension.youtube.sponsorblock.objects.SponsorSegment;

public class SkipSponsorButton extends FrameLayout {
    private final TextView skipSponsorTextView;
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
        final LinearLayout skipSponsorBtnContainer = (LinearLayout) Objects.requireNonNull((View) findViewById(getIdIdentifier("revanced_sb_skip_sponsor_button_container")));  // id:revanced_sb_skip_sponsor_button_container
        skipSponsorTextView = (TextView) Objects.requireNonNull((View) findViewById(getIdIdentifier("revanced_sb_skip_sponsor_button_text")));  // id:revanced_sb_skip_sponsor_button_text;

        skipSponsorBtnContainer.setOnClickListener(v -> {
            // The view controller handles hiding this button, but hide it here as well just in case something goofs.
            setVisibility(View.GONE);
            SegmentPlaybackController.onSkipSegmentClicked(segment);
        });
    }

    /**
     * @return true, if this button state was changed
     */
    public boolean updateSkipButtonText(@NonNull SponsorSegment segment) {
        this.segment = segment;
        final String newText = segment.getSkipButtonText();
        if (newText.equals(skipSponsorTextView.getText().toString())) {
            return false;
        }
        skipSponsorTextView.setText(newText);
        return true;
    }
}
