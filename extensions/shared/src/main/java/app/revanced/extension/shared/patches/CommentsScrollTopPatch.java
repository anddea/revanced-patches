package app.revanced.extension.shared.patches;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.ref.WeakReference;

import app.revanced.extension.shared.settings.BaseSettings;

@SuppressWarnings({"unused", "SameParameterValue"})
public class CommentsScrollTopPatch {
    private static final boolean ENABLE_COMMENTS_SCROLL_TOP =
            BaseSettings.ENABLE_COMMENTS_SCROLL_TOP.get();
    private static volatile WeakReference<RecyclerView> recyclerViewRef =
            new WeakReference<>(null);

    /**
     * Injection point.
     */
    public static boolean isCommentsScrollTopEnabled() {
        return ENABLE_COMMENTS_SCROLL_TOP;
    }

    /**
     * Injection point.
     * Called after {@link #isCommentsScrollTopEnabled()}.
     * @param recyclerView  The parent view to which the comment views are bound.
     */
    public static void onCommentsCreate(RecyclerView recyclerView) {
        recyclerViewRef = new WeakReference<>(recyclerView);
    }

    /**
     * Injection point.
     * @param view  Engagement panel title.
     */
    public static void setContentHeader(View view) {
        if (!ENABLE_COMMENTS_SCROLL_TOP) {
            return;
        }
        view.setOnClickListener(v -> {
            RecyclerView recyclerView = recyclerViewRef.get();
            if (recyclerView != null) {
                smoothScrollToPosition(recyclerView, 0);
            }
        });
    }

    /**
     * Rest of the implementation added by patch.
     */
    private static void smoothScrollToPosition(RecyclerView recyclerView, int position) {
    }
}