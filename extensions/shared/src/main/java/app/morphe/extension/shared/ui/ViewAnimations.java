package app.morphe.extension.shared.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.view.View;

public final class ViewAnimations {
    private ViewAnimations() {}

    /** How far a view shrinks while held. */
    private static final float PRESSED_SCALE = 0.95f;

    private static final long PRESS_DURATION_MILLISECONDS = 50;

    /**
     * Makes a view press in while held, the way the app animates its own buttons.
     * <p>
     * Uses a state list animator rather than a touch listener, so the effect follows
     * the pressed state and never swallows clicks.
     */
    public static void applyPressEffect(View view) {
        StateListAnimator animator = new StateListAnimator();
        animator.addState(new int[]{android.R.attr.state_pressed}, scaleAnimator(PRESSED_SCALE));
        animator.addState(new int[0], scaleAnimator(1f));
        view.setStateListAnimator(animator);
    }

    /**
     * Animates both scale axes together, since a state list animator drives one
     * animator per state.
     */
    private static Animator scaleAnimator(float scale) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(null, View.SCALE_X, scale),
                ObjectAnimator.ofFloat(null, View.SCALE_Y, scale));
        set.setDuration(PRESS_DURATION_MILLISECONDS);
        return set;
    }
}
