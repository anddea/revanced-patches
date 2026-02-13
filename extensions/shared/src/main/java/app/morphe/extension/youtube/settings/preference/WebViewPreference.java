package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.IntentUtils.launchWebViewActivity;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.dipToPixels;

import android.app.Dialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;

import app.morphe.extension.shared.ui.CustomDialog;

@SuppressWarnings({"unused", "deprecation"})
public class WebViewPreference extends Preference implements Preference.OnPreferenceClickListener {
    private final String YOUTUBE_SIGN_IN_URL = "https://www.youtube.com/signin";
    private final String[] labels = {
            "revanced_webview_intent_clear_cookies_on_startup",
            "revanced_webview_intent_clear_cookies_on_shutdown",
            "revanced_webview_intent_use_desktop_user_agent"
    };
    private final boolean[] items = {
            false,
            false,
            false
    };

    private void init() {
        setSelectable(true);
        setOnPreferenceClickListener(this);
    }

    public WebViewPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public WebViewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public WebViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WebViewPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final Context context = getContext();

        // Create the main layout for the dialog content.
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        // Add behavior selection radio buttons.
        LinearLayout checkBoxGroup = new LinearLayout(context);
        checkBoxGroup.setOrientation(RadioGroup.VERTICAL);
        for (int i = 0; i < labels.length; i++) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setText(str(labels[i]));
            checkBox.setId(i);
            checkBox.setChecked(items[i]);
            checkBoxGroup.addView(checkBox);
        }
        checkBoxGroup.setPadding(dipToPixels(10), 0, 0, 0);
        contentLayout.addView(checkBoxGroup);

        TableLayout table = new TableLayout(context);
        table.setOrientation(LinearLayout.HORIZONTAL);
        table.setPadding(15, 0, 15, 0);

        TableRow row = new TableRow(context);

        EditText mEditText = new EditText(context);
        mEditText.setHint(YOUTUBE_SIGN_IN_URL);
        mEditText.setText(YOUTUBE_SIGN_IN_URL);
        mEditText.setSelection(YOUTUBE_SIGN_IN_URL.length());
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_PT, 9);
        mEditText.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(mEditText);

        table.addView(row);
        contentLayout.addView(table);

        // Create ScrollView to wrap the content layout.
        ScrollView contentScrollView = new ScrollView(context);
        contentScrollView.setVerticalScrollBarEnabled(false); // Disable vertical scrollbar.
        contentScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER); // Disable overscroll effect.
        LinearLayout.LayoutParams scrollViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        contentScrollView.setLayoutParams(scrollViewParams);
        contentScrollView.addView(contentLayout);

        // Create the custom dialog.
        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                // Context.
                context,
                // Title.
                str("revanced_webview_title"),
                // Message.
                null,
                // EditText.
                null,
                // OK button text.
                null,
                // OK button action.
                () -> launchWebViewActivity(
                        getContext(),
                        isChecked(checkBoxGroup, 0),
                        isChecked(checkBoxGroup, 1),
                        isChecked(checkBoxGroup, 2),
                        mEditText.getText().toString().trim()
                ),
                // Cancel button action.
                () -> {
                },
                // Neutral button text.
                str("revanced_settings_reset"),
                // Neutral button action.
                () -> {
                    for (int i = 0; i < labels.length; i++) {
                        if (checkBoxGroup.findViewById(i) instanceof CheckBox checkBox) {
                            checkBox.setChecked(items[i]);
                        }
                    }
                    mEditText.setText(YOUTUBE_SIGN_IN_URL);
                    mEditText.setSelection(YOUTUBE_SIGN_IN_URL.length());
                },
                // Dismiss dialog when onNeutralClick.
                false
        );

        // Add the ScrollView to the dialog's main layout.
        LinearLayout dialogMainLayout = dialogPair.second;
        dialogMainLayout.addView(contentScrollView, dialogMainLayout.getChildCount() - 1);
        // Show the dialog.
        dialogPair.first.show();

        return true;
    }

    private boolean isChecked(LinearLayout checkBoxGroup, int index) {
        if (checkBoxGroup.findViewById(index) instanceof CheckBox checkBox) {
            return checkBox.isChecked();
        }
        return false;
    }
}