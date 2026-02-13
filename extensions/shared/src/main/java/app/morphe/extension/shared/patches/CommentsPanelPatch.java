package app.morphe.extension.shared.patches;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import app.morphe.extension.shared.settings.BaseSettings;

@SuppressWarnings({"unused", "SameParameterValue"})
public class CommentsPanelPatch {
    private static final boolean ENABLE_COMMENTS_SCROLL_TOP =
            BaseSettings.ENABLE_COMMENTS_SCROLL_TOP.get();
    private static final boolean HIDE_COMMENTS_INFORMATION_BUTTON =
            BaseSettings.HIDE_COMMENTS_INFORMATION_BUTTON.get();
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
     */
    public static void hideInformationButton(View view) {
        if (HIDE_COMMENTS_INFORMATION_BUTTON) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 0);
            view.setLayoutParams(lp);
        }
    }

    /**
     * Injection point.
     * Called after {@link #isCommentsScrollTopEnabled()}.
     *
     * @param commentsRecyclerView The parent view to which the comment views are bound.
     */
    public static void onCommentsCreate(RecyclerView commentsRecyclerView) {
        if (ENABLE_COMMENTS_SCROLL_TOP) {
            recyclerView = commentsRecyclerView;
        }
    }

    /**
     * Injection point.
     *
     * @param view Engagement panel title.
     */
    public static void setContentHeader(View view) {
        if (ENABLE_COMMENTS_SCROLL_TOP) {
            view.setOnClickListener(v -> {
                if (recyclerView != null) {
                    smoothScrollToPosition(recyclerView, 0);
                }
            });
        }
    }

    /**
     * Rest of the implementation added by patch.
     */
    private static void smoothScrollToPosition(RecyclerView recyclerView, int position) {
    }
}