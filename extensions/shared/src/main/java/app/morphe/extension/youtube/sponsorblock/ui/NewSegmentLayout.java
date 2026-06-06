package app.morphe.extension.youtube.sponsorblock.ui;

import static app.morphe.extension.shared.utils.ResourceUtils.getColor;
import static app.morphe.extension.shared.utils.ResourceUtils.getIdentifier;
import static app.morphe.extension.shared.utils.ResourceUtils.getLayoutIdentifier;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.sponsorblock.SponsorBlockUtils;

public final class NewSegmentLayout extends FrameLayout {
    private static final float SURFACE_Z = 1001f;
    private static final ColorStateList rippleColorStateList = new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_enabled}},
            new int[]{0x33ffffff} // Ripple effect color (semi-transparent white)
    );

    private float dragStartX, dragStartY;
    private float initialTransX, initialTransY;
    private boolean isDragging;
    // Stored squared to compare against squared distance, avoiding sqrt() in the hot path.
    private final int touchSlopSquare;

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

        final int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        touchSlopSquare = touchSlop * touchSlop;
        setClickable(true);
        setFocusable(true);
        keepOnTop();

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
        keepOnTop();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed && !isDragging && getWidth() > 0 && getHeight() > 0) {
            ViewGroup parent = (ViewGroup) getParent();
            if (parent != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
                final long saved = Settings.SB_NEW_SEGMENT_PANEL_POSITION.get();
                setTranslationX(Float.intBitsToFloat((int) (saved >> 32)) * parent.getWidth());
                setTranslationY(Float.intBitsToFloat((int) saved) * parent.getHeight());
                clampTranslationToBounds();
            }
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                keepOnTop();
                startDragTracking(ev);
                requestParentsDisallowInterceptTouchEvent(true);
                return false;
            case MotionEvent.ACTION_MOVE:
                if (!isDragging) {
                    float dx = ev.getRawX() - dragStartX;
                    float dy = ev.getRawY() - dragStartY;
                    if (dx * dx + dy * dy > touchSlopSquare) {
                        isDragging = true;
                        requestParentsDisallowInterceptTouchEvent(true);
                        return true;
                    }
                }
                return isDragging;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                requestParentsDisallowInterceptTouchEvent(false);
                return false;
        }
        return false;
    }

    private void startDragTracking(MotionEvent ev) {
        dragStartX = ev.getRawX();
        dragStartY = ev.getRawY();
        initialTransX = getTranslationX();
        initialTransY = getTranslationY();
        isDragging = false;
    }

    private void keepOnTop() {
        bringToFront();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(SURFACE_Z);
            setTranslationZ(SURFACE_Z);
        }
    }

    private void requestParentsDisallowInterceptTouchEvent(boolean disallowIntercept) {
        ViewParent parent = getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
            parent = parent.getParent();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                keepOnTop();
                startDragTracking(ev);
                requestParentsDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                setTranslationX(initialTransX + (ev.getRawX() - dragStartX));
                setTranslationY(initialTransY + (ev.getRawY() - dragStartY));
                return true;
            case MotionEvent.ACTION_UP:
                isDragging = false;
                requestParentsDisallowInterceptTouchEvent(false);
                clampTranslationToBounds();
                saveRelativePosition();
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                requestParentsDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    private void clampTranslationToBounds() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) return;

        final int height = getHeight();
        final int width = getWidth();
        if (width == 0 || height == 0) return;

        final int top = getTop();
        final int left = getLeft();
        final float transX = Math.max(-left, Math.min(getTranslationX(), parent.getWidth() - left - width));
        final float transY = Math.max(-top, Math.min(getTranslationY(), parent.getHeight() - top - height));
        setTranslationX(transX);
        setTranslationY(transY);
    }

    private void saveRelativePosition() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null || parent.getWidth() == 0 || parent.getHeight() == 0) return;

        Settings.SB_NEW_SEGMENT_PANEL_POSITION.save(
                (long) Float.floatToIntBits(getTranslationX() / parent.getWidth()) << 32
                        | (Float.floatToIntBits(getTranslationY() / parent.getHeight()) & 0xFFFFFFFFL));
    }

    @FunctionalInterface
    private interface ButtonOnClickHandlerFunction {
        void apply();
    }
}
