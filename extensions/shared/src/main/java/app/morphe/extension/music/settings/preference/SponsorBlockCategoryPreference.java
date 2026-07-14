/*
 * Portions of this file are adapted from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.music.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.dipToPixels;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;

import app.morphe.extension.music.sponsorblock.objects.CategoryBehaviour;
import app.morphe.extension.music.sponsorblock.objects.SegmentCategory;
import app.morphe.extension.shared.settings.preference.ColorPickerPreference;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * XML-backed SponsorBlock category preference for YouTube Music.
 *
 * <p>Each preference persists the category color through the shared color picker and adds the
 * category behavior controls used by SponsorBlock. The category is resolved from the color
 * setting key after preference inflation so this class can be reused for every segment.</p>
 */
@SuppressWarnings({"deprecation", "unused"})
public class SponsorBlockCategoryPreference extends ColorPickerPreference {
    @Nullable
    private SegmentCategory category;

    private int selectedDialogEntryIndex;

    public SponsorBlockCategoryPreference(Context context) {
        super(context);
        initialize();
    }

    public SponsorBlockCategoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public SponsorBlockCategoryPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setOpacitySliderEnabled(true);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);

        if (category == null) {
            category = categoryByColorSettingKey(getKey());
            if (category == null) {
                throw new IllegalStateException(
                        "SponsorBlock category preference has no matching setting: " + getKey());
            }
            setTitle(category.title.toString());
            setSummary(category.description.toString());
        }

        setText(category.getColorStringWithOpacity());
    }

    @Override
    public final void setText(String colorString) {
        try {
            colorString = getColorString(Color.parseColor(colorString), true);
            super.setText(colorString);

            if (category != null) {
                category.setColorWithOpacity(colorString);
            }
        } catch (IllegalArgumentException ex) {
            Utils.showToastShort(str("revanced_settings_color_invalid"));
            if (category != null) {
                setText(category.colorSetting.defaultValue);
            }
        } catch (Exception ex) {
            String colorStringFinal = colorString;
            Logger.printException(() -> "setText failure: " + colorStringFinal, ex);
        }
    }

    @Nullable
    @Override
    protected View createExtraDialogContentView(Context context) {
        if (category == null) return null;

        CategoryBehaviour[] behaviours = CategoryBehaviour.values();
        selectedDialogEntryIndex = 0;
        for (int i = 0; i < behaviours.length; i++) {
            if (behaviours[i] == category.behaviour) {
                selectedDialogEntryIndex = i;
                break;
            }
        }

        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        for (int i = 0; i < behaviours.length; i++) {
            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(behaviours[i].description.toString());
            radioButton.setId(i);
            radioButton.setChecked(i == selectedDialogEntryIndex);
            radioGroup.addView(radioButton);
        }

        radioGroup.setOnCheckedChangeListener(
                (group, checkedId) -> selectedDialogEntryIndex = checkedId);
        radioGroup.setPadding(dipToPixels(10), 0, dipToPixels(10), dipToPixels(10));
        return radioGroup;
    }

    @Override
    protected void onDialogOkClicked() {
        if (category == null
                || selectedDialogEntryIndex < 0
                || selectedDialogEntryIndex >= CategoryBehaviour.values().length) {
            return;
        }

        category.setBehaviour(CategoryBehaviour.values()[selectedDialogEntryIndex]);
        SegmentCategory.updateEnabledCategories();
    }

    @Override
    protected void onDialogNeutralClicked() {
        if (category == null) return;
        dialogColorPickerView.setColor(category.getDefaultColorWithOpacity());
    }

    @Nullable
    private static SegmentCategory categoryByColorSettingKey(@Nullable String colorSettingKey) {
        if (colorSettingKey == null) return null;

        for (SegmentCategory category : SegmentCategory.categoriesWithoutUnsubmitted()) {
            if (colorSettingKey.equals(category.colorSetting.key)) {
                return category;
            }
        }
        return null;
    }
}
