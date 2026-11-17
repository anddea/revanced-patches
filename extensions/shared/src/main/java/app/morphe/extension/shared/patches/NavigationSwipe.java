package app.morphe.extension.shared.patches;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Observes the activity window so horizontal tab gestures can start anywhere on screen.
 * Uses the live tab views rather than an enum list, preserving custom actions and visual order.
 */
public final class NavigationSwipe {
    private static final WeakHashMap<Window, NavigationSwipe> windows = new WeakHashMap<>();
    private final List<WeakReference<View>> tabs = new ArrayList<>();
    private float startX, startY;
    private boolean tracking, consumed;

    private NavigationSwipe() { }

    /** Register a tab without retaining its activity after the window is destroyed. */
    public static void register(View tab) {
        Context context = tab.getContext();
        while (context instanceof ContextWrapper && !(context instanceof Activity)) {
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) return;
            context = base;
        }
        if (!(context instanceof Activity)) return;
        Window window = ((Activity) context).getWindow();
        NavigationSwipe swipe = windows.get(window);
        if (swipe == null) {
            swipe = new NavigationSwipe();
            NavigationSwipe handler = swipe;
            Window.Callback original = window.getCallback();
            if (original == null) return;
            window.setCallback((Window.Callback) Proxy.newProxyInstance(
                    Window.Callback.class.getClassLoader(), new Class<?>[]{Window.Callback.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("dispatchTouchEvent")
                                && handler.onTouch((MotionEvent) args[0], original)) return true;
                        try {
                            return method.invoke(original, args);
                        } catch (InvocationTargetException ex) {
                            throw ex.getCause();
                        }
                    }));
            windows.put(window, swipe);
        }
        for (WeakReference<View> ref : swipe.tabs) {
            if (ref.get() == tab) return;
        }
        swipe.tabs.removeIf(ref -> ref.get() == null);
        swipe.tabs.add(new WeakReference<>(tab));
    }

    private static boolean selected(View view) {
        if (view.isSelected() || view.isActivated()) return true;
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                if (selected(group.getChildAt(i))) return true;
            }
        }
        return false;
    }

    private List<View> visibleTabs() {
        List<View> visible = new ArrayList<>();
        for (WeakReference<View> ref : tabs) {
            View tab = ref.get();
            if (tab != null && tab.isShown() && tab.isEnabled()
                    && tab.getWidth() > 0 && tab.getHeight() > 0) visible.add(tab);
        }
        visible.sort(Comparator.comparingInt(view -> {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            return location[0];
        }));
        return visible;
    }

    /**
     * Reserve gestures starting on horizontal shelves for their native scroll handler.
     * Check both directions at touch down so reaching a shelf edge cannot switch tabs.
     */
    private static boolean touchesHorizontalScroller(View view, int x, int y) {
        Rect bounds = new Rect();
        if (!view.isShown() || !view.getGlobalVisibleRect(bounds) || !bounds.contains(x, y)) {
            return false;
        }
        if (view.canScrollHorizontally(-1) || view.canScrollHorizontally(1)) return true;
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                if (touchesHorizontalScroller(group.getChildAt(i), x, y)) return true;
            }
        }
        return false;
    }

    /** Cancel the original stream before activating a tab, avoiding a second click on release. */
    private boolean onTouch(MotionEvent event, Window.Callback original) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            consumed = false;
            List<View> visible = visibleTabs();
            tracking = visible.size() > 1 && !touchesHorizontalScroller(
                    visible.get(0).getRootView(), (int) event.getRawX(), (int) event.getRawY());
            startX = event.getX();
            startY = event.getY();
        } else if (consumed) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) consumed = false;
            return true;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_UP) {
            tracking = false;
        } else if (tracking && action == MotionEvent.ACTION_MOVE) {
            float dx = event.getX() - startX;
            float dy = event.getY() - startY;
            float density = android.content.res.Resources.getSystem().getDisplayMetrics().density;
            if (Math.abs(dy) > 24 * density && Math.abs(dy) >= Math.abs(dx)) tracking = false;
            if (Math.abs(dx) < 72 * density || Math.abs(dx) < Math.abs(dy) * 2) return false;
            tracking = false;
            List<View> visible = visibleTabs();
            if (visible.size() < 2) return false;
            int current = -1;
            for (int i = 0; i < visible.size(); i++) {
                if (selected(visible.get(i))) {
                    current = i;
                    break;
                }
            }
            // A hidden tab can still be the app's initial selection. Enter the visible tabs
            // from the appropriate edge instead of requiring a manual tap first.
            int next = current < 0 ? (dx < 0 ? 0 : visible.size() - 1)
                    : current + (dx < 0 ? 1 : -1);
            if (next < 0 || next >= visible.size()) return false;
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            original.dispatchTouchEvent(cancel);
            cancel.recycle();
            consumed = true;
            View target = visible.get(next);
            // Dispatch a real tap: replacement Music tabs use an OnTouchListener.
            long now = SystemClock.uptimeMillis();
            MotionEvent tap = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN,
                    target.getWidth() / 2f, target.getHeight() / 2f, 0);
            target.dispatchTouchEvent(tap);
            tap.setAction(MotionEvent.ACTION_UP);
            target.dispatchTouchEvent(tap);
            tap.recycle();
            return true;
        }
        return false;
    }
}
