package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.preference.ListPreference;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Objects;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.sponsorblock.objects.CategoryBehaviour;
import app.revanced.extension.youtube.sponsorblock.objects.SegmentCategory;

@SuppressWarnings({"unused", "deprecation"})
public class SegmentCategoryListPreference extends ListPreference {
    private SegmentCategory mCategory;
    private EditText mEditText;
    private int mClickedDialogEntryIndex;

    private void init() {
        final SegmentCategory segmentCategory = SegmentCategory.byCategoryKey(getKey());
        final boolean isHighlightCategory = segmentCategory == SegmentCategory.HIGHLIGHT;
        mCategory = Objects.requireNonNull(segmentCategory);
        // Edit: Using preferences to sync together multiple pieces
        // of code together is messy and should be rethought.
        setKey(segmentCategory.behaviorSetting.key);
        setDefaultValue(segmentCategory.behaviorSetting.defaultValue);

        setEntries(isHighlightCategory
                ? CategoryBehaviour.getBehaviorDescriptionsWithoutSkipOnce()
                : CategoryBehaviour.getBehaviorDescriptions());
        setEntryValues(isHighlightCategory
                ? CategoryBehaviour.getBehaviorKeyValuesWithoutSkipOnce()
                : CategoryBehaviour.getBehaviorKeyValues());
        updateTitle();
    }

    public SegmentCategoryListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SegmentCategoryListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SegmentCategoryListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SegmentCategoryListPreference(Context context) {
        super(context);
        init();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        try {
            Utils.setEditTextDialogTheme(builder);
            super.onPrepareDialogBuilder(builder);

            Context context = builder.getContext();
            TableLayout table = new TableLayout(context);
            table.setOrientation(LinearLayout.HORIZONTAL);
            table.setPadding(70, 0, 150, 0);

            TableRow row = new TableRow(context);

            TextView colorTextLabel = new TextView(context);
            colorTextLabel.setText(str("revanced_sb_color_dot_label"));
            row.addView(colorTextLabel);

            TextView colorDotView = new TextView(context);
            colorDotView.setText(mCategory.getCategoryColorDot());
            colorDotView.setPadding(30, 0, 30, 0);
            row.addView(colorDotView);

            mEditText = new EditText(context);
            mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            mEditText.setText(mCategory.colorString());
            mEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        String colorString = s.toString();
                        if (!colorString.startsWith("#")) {
                            s.insert(0, "#"); // recursively calls back into this method
                            return;
                        }
                        if (colorString.length() > 7) {
                            s.delete(7, colorString.length());
                            return;
                        }
                        final int color = Color.parseColor(colorString);
                        colorDotView.setText(SegmentCategory.getCategoryColorDot(color));
                    } catch (IllegalArgumentException ex) {
                        // ignore
                    }
                }
            });
            mEditText.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(mEditText);

            table.addView(row);
            builder.setView(table);
            builder.setTitle(mCategory.title.toString());

            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> onClick(dialog, DialogInterface.BUTTON_POSITIVE));
            builder.setNeutralButton(str("revanced_sb_reset_color"), (dialog, which) -> {
                try {
                    mCategory.resetColor();
                    updateTitle();
                    Utils.showToastShort(str("revanced_sb_color_reset"));
                } catch (Exception ex) {
                    Logger.printException(() -> "setNeutralButton failure", ex);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            mClickedDialogEntryIndex = findIndexOfValue(getValue());
            builder.setSingleChoiceItems(getEntries(), mClickedDialogEntryIndex, (dialog, which) -> mClickedDialogEntryIndex = which);
        } catch (Exception ex) {
            Logger.printException(() -> "onPrepareDialogBuilder failure", ex);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        try {
            if (positiveResult && mClickedDialogEntryIndex >= 0 && getEntryValues() != null) {
                String value = getEntryValues()[mClickedDialogEntryIndex].toString();
                if (callChangeListener(value)) {
                    setValue(value);
                    mCategory.setBehaviour(Objects.requireNonNull(CategoryBehaviour.byReVancedKeyValue(value)));
                    SegmentCategory.updateEnabledCategories();
                }
                String colorString = mEditText.getText().toString();
                try {
                    if (!colorString.equals(mCategory.colorString())) {
                        mCategory.setColor(colorString);
                        Utils.showToastShort(str("revanced_sb_color_changed"));
                    }
                } catch (IllegalArgumentException ex) {
                    Utils.showToastShort(str("revanced_sb_color_invalid"));
                }
                updateTitle();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onDialogClosed failure", ex);
        }
    }

    private void updateTitle() {
        setTitle(mCategory.getTitleWithColorDot());
        setEnabled(Settings.SB_ENABLED.get());
    }
}