package app.revanced.extension.shared.patches;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import app.revanced.extension.shared.settings.BaseSettings;

@SuppressWarnings({"unused", "SameParameterValue"})
public class CommentsScrollTopPatch {
    private static final boolean ENABLE_COMMENTS_SCROLL_TOP =
            BaseSettings.ENABLE_COMMENTS_SCROLL_TOP.get();
    @SuppressLint("StaticFieldLeak")
    private static RecyclerView recyclerView;

    /**
     * Injection point.
     */
    public static boolean isCommentsScrollTopEnabled() {
        return ENABLE_COMMENTS_SCROLL_TOP;
    }

    /**
     * Injection point.
     * Called after {@link #isCommentsScrollTopEnabled()}.
     *
     * @param commentsRecyclerView The parent view to which the comment views are bound.
     */
    public static void onCommentsCreate(RecyclerView commentsRecyclerView) {
        recyclerView = commentsRecyclerView;
    }

    /**
     * Injection point.
     *
     * @param view Engagement panel title.
     */
    public static void setContentHeader(View view) {
        if (!ENABLE_COMMENTS_SCROLL_TOP) {
            return;
        }
        view.setOnClickListener(v -> {
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