package app.morphe.extension.youtube.sponsorblock.ui;

import static app.morphe.extension.shared.utils.ResourceUtils.getColor;
import static app.morphe.extension.shared.utils.ResourceUtils.getIdentifier;
import static app.morphe.extension.shared.utils.ResourceUtils.getLayoutIdentifier;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import android.widget.ImageView;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.sponsorblock.SponsorBlockUtils;

public final class NewSegmentLayout extends FrameLayout {
    private static final ColorStateList rippleColorStateList = new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_enabled}},
            new int[]{0x33ffffff} // Ripple effect color (semi-transparent white)
    );

    private float dX, dY;
    private boolean isDragging = false;
    private ImageView dragHandle;

    public NewSegmentLayout(final Context context) {
        this(context, null);
    }

    public NewSegmentLayout(final Context context, final AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public NewSegmentLayout(final Context context, final AttributeSet attributeSet, final int defStyleAttr) {
        this(context, attributeSet, defStyleAttr, 0);
    }

    public NewSegmentLayout(final Context context, final AttributeSet attributeSet,
                            final int defStyleAttr, final int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(getLayoutIdentifier("revanced_sb_new_segment"), this, true);

        initializeButton(
                context,
                "revanced_sb_new_segment_rewind",
                () -> VideoInformation.seekToRelative(-Settings.SB_CREATE_NEW_SEGMENT_STEP.get()),
                "Rewind button clicked"
        );

        initializeButton(
                context,
                "revanced_sb_new_segment_forward",
                () -> VideoInformation.seekToRelative(Settings.SB_CREATE_NEW_SEGMENT_STEP.get()),
                "Forward button clicked"
        );

        initializeButton(
                context,
                "revanced_sb_new_segment_adjust",
                SponsorBlockUtils::onMarkLocationClicked,
                "Adjust button clicked"
        );

        initializeButton(
                context,
                "revanced_sb_new_segment_compare",
                SponsorBlockUtils::onPreviewClicked,
                "Compare button clicked"
        );

        initializeButton(
                context,
                "revanced_sb_new_segment_edit",
                SponsorBlockUtils::onEditByHandClicked,
                "Edit button clicked"
        );

        initializeButton(
                context,
                "revanced_sb_new_segment_publish",
                SponsorBlockUtils::onPublishClicked,
                "Publish button clicked"
        );
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        dragHandle = findViewById(getIdentifier("revanced_sb_new_segment_drag_handle", ResourceUtils.ResourceType.ID, getContext()));
        setupDragHandle();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDragHandle() {
        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX = getX() - event.getRawX();
                    dY = getY() - event.getRawY();
                    isDragging = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        setY(event.getRawY() + dY);
                        setX(event.getRawX() + dX);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDragging = false;
                    break;
            }
            return true;
        });
    }

    /**
     * Initializes a segment button with the given resource identifier name with the given handler and a ripple effect.
     *
     * @param context                The context.
     * @param resourceIdentifierName The resource identifier name for the button.
     * @param handler                The handler for the button's click event.
     * @param debugMessage           The debug message to print when the button is clicked.
     */
    private void initializeButton(final Context context, final String resourceIdentifierName,
                                  final ButtonOnClickHandlerFunction handler, final String debugMessage) {
        ImageButton button = findViewById(getIdentifier(resourceIdentifierName, ResourceUtils.ResourceType.ID, context));

        // Add ripple effect
        RippleDrawable rippleDrawable = new RippleDrawable(
                rippleColorStateList, null, null
        );
        button.setBackground(rippleDrawable);
        button.setOnClickListener((v) -> {
            handler.apply();
            Logger.printDebug(() -> debugMessage);
        });
    }

    /**
     * Update the layout of this UI control.
     */
    public void updateLayout() {
        final boolean squareLayout = Settings.SB_SQUARE_LAYOUT.get();

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) getLayoutParams();
        final int margin = squareLayout
                ? 0
                : SponsorBlockViewController.ROUNDED_LAYOUT_MARGIN;
        params.setMarginStart(margin);
        setLayoutParams(params);

        GradientDrawable backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setColor(getColor("skip_ad_button_background_color"));
        final float cornerRadius = squareLayout
                ? 0
                : 16 * getResources().getDisplayMetrics().density;
        backgroundDrawable.setCornerRadius(cornerRadius);
        setBackground(backgroundDrawable);
    }

    @FunctionalInterface
    private interface ButtonOnClickHandlerFunction {
        void apply();
    }
}
