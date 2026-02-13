package app.morphe.extension.shared.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("DiscouragedApi")
public class StringRef extends Utils {
    private static Resources resources;

    // must use a thread safe map, as this class is used both on and off the main thread
    private static final Map<String, StringRef> strings = Collections.synchronizedMap(new HashMap<>());

    /**
     * Returns a cached instance.
     * Should be used if the same String could be loaded more than once.
     *
     * @param id string resource name/id
     * @see #sf(String)
     */
    @NonNull
    public static StringRef sfc(@NonNull String id) {
        StringRef ref = strings.get(id);
        if (ref == null) {
            ref = new StringRef(id);
            strings.put(id, ref);
        }
        return ref;
    }

    /**
     * Creates a new instance, but does not cache the value.
     * Should be used for Strings that are loaded exactly once.
     *
     * @param id string resource name/id
     * @see #sfc(String)
     */
    @NonNull
    public static StringRef sf(@NonNull String id) {
        return new StringRef(id);
    }

    /**
     * Gets string value by string id, shorthand for <code>sfc(id).toString()</code>
     *
     * @param id string resource name/id
     * @return String value from string.xml
     */
    @NonNull
    public static String str(@NonNull String id) {
        return sfc(id).toString();
    }

    /**
     * Gets string value by string id, shorthand for <code>sfc(id).toString()</code> and formats the string
     * with given args.
     *
     * @param id   string resource name/id
     * @param args the args to format the string with
     * @return String value from string.xml formatted with given args
     */
    @NonNull
    public static String str(@NonNull String id, Object... args) {
        return String.format(str(id), args);
    }

    // Dynamic string
    public static int dstr(@NonNull String resName) {
        try {
            Activity mActivity = getActivity();
            Context context = mActivity != null ? mActivity : getContext();

            if (context != null) {
                return context.getResources().getIdentifier(resName, "string", context.getPackageName());
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Error getting resource ID for: " + resName, ex);
        }
        return 0;
    }

    /**
     * Creates a StringRef object that'll not change it's value
     *
     * @param value value which toString() method returns when invoked on returned object
     * @return Unique StringRef instance, its value will never change
     */
    @NonNull
    public static StringRef constant(@NonNull String value) {
        final StringRef ref = new StringRef(value);
        ref.resolved = true;
        return ref;
    }

    /**
     * Shorthand for <code>constant("")</code>
     * Its value always resolves to empty string
     */
    @SuppressLint("StaticFieldLeak")
    @NonNull
    public static final StringRef empty = constant("");

    @NonNull
    private String value;
    private boolean resolved;

    public StringRef(@NonNull String resName) {
        this.value = resName;
    }

    @Override
    @NonNull
    public String toString() {
        if (!resolved) {
            try {
                Activity mActivity = getActivity();
                Context context = mActivity != null
                        ? mActivity
                        : getContext();
                if (resources == null) {
                    resources = context.getResources();
                }
                if (resources != null) {
                    final String packageName = context.getPackageName();
                    final int identifier = resources.getIdentifier(value, "string", packageName);
                    if (identifier == 0)
                        Logger.printException(() -> "Resource not found: " + value);
                    else
                        value = resources.getString(identifier);
                    resolved = true;
                } else {
                    Logger.printException(() -> "Could not resolve resources!");
                }
            } catch (Exception ex) {
                Logger.printException(() -> "Context is null!", ex);
            }
        }

        return value;
    }
}
