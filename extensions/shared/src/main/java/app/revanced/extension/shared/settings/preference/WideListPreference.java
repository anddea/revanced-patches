package app.revanced.extension.shared.settings.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings({"unused", "deprecation"})
public class WideListPreference extends ListPreference {

    public WideListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public WideListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WideListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WideListPreference(Context context) {
        super(context);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        Utils.setEditTextDialogTheme(builder, true);
        super.onPrepareDialogBuilder(builder);
    }
}
