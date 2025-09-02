package com.liskovsoft.googlecommon.common.js;

import androidx.annotation.Nullable;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8ResultUndefined;
import com.eclipsesource.v8.V8ScriptExecutionException;

import java.util.List;

import app.revanced.extension.shared.utils.Logger;

public final class V8Runtime {
    private static V8Runtime sInstance;
    private V8 mRuntime;

    //static {
    //    // Fix? J2V8 native library not loaded (j2v8-android-arm_32/j2v8-android-arm_32)
    //    System.loadLibrary("j2v8");
    //}

    private V8Runtime() {
    }

    public static V8Runtime instance() {
        if (sInstance == null) {
            sInstance = new V8Runtime();
        }

        return sInstance;
    }

    public static void unhold() {
        // NOTE: using 'release' produces 'Invalid V8 thread access: the locker has been released!'
        //if (sInstance != null) {
        //    sInstance.mRuntime.release();
        //}

        sInstance = null;
    }

    @Nullable
    public String evaluate(final String source) {
        try {
            return evaluateSafe(source);
        } catch (V8ScriptExecutionException e) {
            Logger.printException(() -> "evaluate(String) failed", e);
        }

        return null;
    }

    @Nullable
    public String evaluateWithErrors(final String source) throws V8ScriptExecutionException {
        return evaluateSafe(source);
    }

    @Nullable
    public String evaluate(final List<String> sources) {
        try {
            return evaluateSafe(sources);
        } catch (V8ScriptExecutionException e) {
            Logger.printException(() -> "evaluate(List) failed", e);
        }

        return null;
    }

    @Nullable
    public String evaluateWithErrors(final List<String> sources) throws V8ScriptExecutionException {
        return evaluateSafe(sources);
    }

    /**
     * Not a thread safe. Possible 'Invalid V8 thread access' errors.
     */
    private String evaluateUnsafe(final String source) throws V8ScriptExecutionException {
        String result = null;

        try {
            if (mRuntime == null) {
                mRuntime = V8.createV8Runtime();
            }
            mRuntime.getLocker().acquire(); // Possible 'Invalid V8 thread access' errors
            result = mRuntime.executeStringScript(source);
        } finally {
            if (mRuntime != null) {
                mRuntime.getLocker().release(); // Possible 'Invalid V8 thread access' errors
            }
        }

        return result;
    }

    /**
     * Thread safe solution but performance a bit slow.
     */
    private String evaluateSafe(final String source) throws V8ScriptExecutionException {
        V8 runtime = null;
        String result;

        try {
            runtime = V8.createV8Runtime();
            result = runtime.executeStringScript(source);
        } finally {
            if (runtime != null) {
                runtime.release(false);
                runtime.close();
            }
        }

        return result;
    }

    /**
     * Thread safe solution but performance a bit slow.
     */
    private String evaluateSafe(final List<String> sources) throws V8ScriptExecutionException {
        V8 runtime = null;
        String result = null;

        try {
            runtime = V8.createV8Runtime();
            for (String source : sources) {
                try {
                    result = runtime.executeStringScript(source);
                } catch (V8ResultUndefined e) {
                    // NOP
                }
            }
        } finally {
            if (runtime != null) {
                runtime.release(false);
                runtime.close();
            }
        }

        return result;
    }
}
