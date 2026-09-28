package app.morphe.extension.shared.utils;

import java.util.ArrayDeque;

public class Utils {
    public static class Context {
        public java.io.File getCacheDir() {
            java.io.File dir =
                    new java.io.File(System.getProperty("java.io.tmpdir"), "rvx-pause-tests");
            dir.mkdirs();
            return dir;
        }
    }

    public static Context getContext() {
        return new Context();
    }

    public static final ArrayDeque<Runnable> background = new ArrayDeque<>();

    public static void runOnBackgroundThread(Runnable r) {
        background.add(r);
    }

    public static boolean mainThread = true;
    public static final ArrayDeque<Runnable> queue = new ArrayDeque<>();

    public static void runOnMainThread(Runnable r) {
        queue.add(r);
    }

    public static void verifyOnMainThread() {
        if (!mainThread) throw new IllegalStateException("off main thread");
    }

    public static void drain() {
        int n = 0;
        while (!queue.isEmpty()) {
            if (n++ > 100) throw new AssertionError("Callback loop");
            queue.remove().run();
        }
    }
}
