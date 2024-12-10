package app.revanced.extension.youtube.sponsorblock.ui;

import static app.revanced.extension.shared.utils.ResourceUtils.getIdentifier;
import static app.revanced.extension.shared.utils.ResourceUtils.getLayoutIdentifier;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import android.widget.ImageView;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;
import app.revanced.extension.youtube.sponsorblock.SponsorBlockUtils;

public final class NewSegmentLayout extends FrameLayout {
    private static final ColorStateList rippleColorStateList = new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_enabled}},
            new int[]{0x33ffffff} // sets the ripple color to white
    );
    private final int rippleEffectId;

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


        TypedValue rippleEffect = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, rippleEffect, true);
        rippleEffectId = rippleEffect.resourceId;

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
        final ImageButton button = findViewById(getIdentifier(resourceIdentifierName, ResourceUtils.ResourceType.ID, context));

        // Add ripple effect
        button.setBackgroundResource(rippleEffectId);
        RippleDrawable rippleDrawable = (RippleDrawable) button.getBackground();
        rippleDrawable.setColor(rippleColorStateList);

        button.setOnClickListener((v) -> {
            handler.apply();
            Logger.printDebug(() -> debugMessage);
        });
    }

    @FunctionalInterface
    public interface ButtonOnClickHandlerFunction {
        void apply();
    }
}
