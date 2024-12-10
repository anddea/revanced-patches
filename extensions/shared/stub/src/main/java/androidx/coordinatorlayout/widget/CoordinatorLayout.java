package androidx.coordinatorlayout.widget;

import android.content.Context;
import android.view.ViewGroup;

/**
 * "CompileOnly" class
 * <p>
 * This class will not be included and "replaced" by the real package's class.
 */
public class CoordinatorLayout extends ViewGroup {
    public CoordinatorLayout(Context context) {
        super(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }
}
