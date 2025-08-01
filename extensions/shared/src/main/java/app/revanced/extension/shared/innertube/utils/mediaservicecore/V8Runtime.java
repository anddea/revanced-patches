package app.revanced.extension.shared.innertube.utils.mediaservicecore;

import androidx.annotation.Nullable;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8ScriptExecutionException;

import app.revanced.extension.shared.utils.Logger;

/**
 * Powered by [MediaServiceCore](https://github.com/yuliskov/MediaServiceCore/blob/f5691d30c81342548852c6951bc7ea5bb8a810ca/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/common/js/V8Runtime.java)
 */
public final class V8Runtime {
    private static V8Runtime sInstance;

    private V8Runtime() {
    }

    public static V8Runtime instance() {
        if (sInstance == null) {
            sInstance = new V8Runtime();
        }

        return sInstance;
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
}
