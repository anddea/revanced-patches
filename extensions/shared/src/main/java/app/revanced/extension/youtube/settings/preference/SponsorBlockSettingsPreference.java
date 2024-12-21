package app.revanced.extension.youtube.settings.preference;

import static android.text.Html.fromHtml;
import static app.revanced.extension.shared.utils.ResourceUtils.getLayoutIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.preference.ResettableEditTextPreference;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.utils.PatchStatus;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.sponsorblock.SegmentPlaybackController;
import app.revanced.extension.youtube.sponsorblock.SponsorBlockSettings;
import app.revanced.extension.youtube.sponsorblock.SponsorBlockUtils;
import app.revanced.extension.youtube.sponsorblock.objects.SegmentCategory;
import app.revanced.extension.youtube.sponsorblock.objects.UserStats;
import app.revanced.extension.youtube.sponsorblock.requests.SBRequester;
import app.revanced.extension.youtube.sponsorblock.ui.SponsorBlockViewController;

@SuppressWarnings({"unused", "deprecation"})
public class SponsorBlockSettingsPreference extends ReVancedPreferenceFragment {

    private static PreferenceCategory statsCategory;

    private static final int preferencesCategoryLayout = getLayoutIdentifier("revanced_settings_preferences_category");

    private static final Preference.OnPreferenceChangeListener updateUI = (pref, newValue) -> {
        updateUI();

        return true;
    };

    @NonNull
    private static SwitchPreference findSwitchPreference(BooleanSetting setting) {
        final String key = setting.key;
        if (mPreferenceManager.findPreference(key) instanceof SwitchPreference switchPreference) {
            switchPreference.setOnPreferenceChangeListener(updateUI);
            return switchPreference;
        } else {
            throw new IllegalStateException("SwitchPreference is null: " + key);
        }
    }

    @NonNull
    private static ResettableEditTextPreference findResettableEditTextPreference(Setting<?> setting) {
        final String key = setting.key;
        if (mPreferenceManager.findPreference(key) instanceof ResettableEditTextPreference switchPreference) {
            switchPreference.setOnPreferenceChangeListener(updateUI);
            return switchPreference;
        } else {
            throw new IllegalStateException("ResettableEditTextPreference is null: " + key);
        }
    }

    public static void updateUI() {
        if (!Settings.SB_ENABLED.get()) {
            SponsorBlockViewController.hideAll();
            SegmentPlaybackController.clearData();
        } else if (!Settings.SB_CREATE_NEW_SEGMENT.get()) {
            SponsorBlockViewController.hideNewSegmentLayout();
        }
    }

    @TargetApi(26)
    public static void init(Activity mActivity) {
        if (!PatchStatus.SponsorBlock()) {
            return;
        }

        final SwitchPreference sbEnabled = findSwitchPreference(Settings.SB_ENABLED);
        sbEnabled.setOnPreferenceClickListener(preference -> {
            updateUI();
            fetchAndDisplayStats();
            updateSegmentCategories();
            return false;
        });

        if (!(sbEnabled.getParent() instanceof PreferenceScreen mPreferenceScreen)) {
            return;
        }

        final SwitchPreference votingEnabled = findSwitchPreference(Settings.SB_VOTING_BUTTON);
        final SwitchPreference compactSkipButton = findSwitchPreference(Settings.SB_COMPACT_SKIP_BUTTON);
        final SwitchPreference autoHideSkipSegmentButton = findSwitchPreference(Settings.SB_AUTO_HIDE_SKIP_BUTTON);
        final SwitchPreference showSkipToast = findSwitchPreference(Settings.SB_TOAST_ON_SKIP);
        showSkipToast.setOnPreferenceClickListener(preference -> {
            Utils.showToastShort(str("revanced_sb_skipped_sponsor"));
            return false;
        });

        final SwitchPreference showTimeWithoutSegments = findSwitchPreference(Settings.SB_VIDEO_LENGTH_WITHOUT_SEGMENTS);

        final SwitchPreference addNewSegment = findSwitchPreference(Settings.SB_CREATE_NEW_SEGMENT);
        addNewSegment.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((Boolean) newValue && !Settings.SB_SEEN_GUIDELINES.get()) {
                Context context = preference.getContext();
                new AlertDialog.Builder(context)
                        .setTitle(str("revanced_sb_guidelines_popup_title"))
                        .setMessage(str("revanced_sb_guidelines_popup_content"))
                        .setNegativeButton(str("revanced_sb_guidelines_popup_already_read"), null)
                        .setPositiveButton(str("revanced_sb_guidelines_popup_open"), (dialogInterface, i) -> openGuidelines(context))
                        .setOnDismissListener(dialog -> Settings.SB_SEEN_GUIDELINES.save(true))
                        .setCancelable(false)
                        .show();
            }
            updateUI();
            return true;
        });

        final ResettableEditTextPreference newSegmentStep = findResettableEditTextPreference(Settings.SB_CREATE_NEW_SEGMENT_STEP);
        newSegmentStep.setOnPreferenceChangeListener((preference, newValue) -> {
            try {
                final int newAdjustmentValue = Integer.parseInt(newValue.toString());
                if (newAdjustmentValue != 0) {
                    Settings.SB_CREATE_NEW_SEGMENT_STEP.save(newAdjustmentValue);
                    return true;
                }
            } catch (NumberFormatException ex) {
                Logger.printInfo(() -> "Invalid new segment step", ex);
            }

            Utils.showToastLong(str("revanced_sb_general_adjusting_invalid"));
            updateUI();
            return false;
        });
        final Preference guidelinePreferences = Objects.requireNonNull(mPreferenceManager.findPreference("revanced_sb_guidelines_preference"));
        guidelinePreferences.setDependency(Settings.SB_ENABLED.key);
        guidelinePreferences.setOnPreferenceClickListener(preference -> {
            openGuidelines(preference.getContext());
            return true;
        });

        final SwitchPreference toastOnConnectionError = findSwitchPreference(Settings.SB_TOAST_ON_CONNECTION_ERROR);
        final SwitchPreference trackSkips = findSwitchPreference(Settings.SB_TRACK_SKIP_COUNT);
        final ResettableEditTextPreference minSegmentDuration = findResettableEditTextPreference(Settings.SB_SEGMENT_MIN_DURATION);
        minSegmentDuration.setOnPreferenceChangeListener((preference, newValue) -> {
            try {
                Float minTimeDuration = Float.valueOf(newValue.toString());
                Settings.SB_SEGMENT_MIN_DURATION.save(minTimeDuration);
                return true;
            } catch (NumberFormatException ex) {
                Logger.printInfo(() -> "Invalid minimum segment duration", ex);
            }

            Utils.showToastLong(str("revanced_sb_general_min_duration_invalid"));
            updateUI();
            return false;
        });
        final ResettableEditTextPreference privateUserId = findResettableEditTextPreference(Settings.SB_PRIVATE_USER_ID);
        privateUserId.setOnPreferenceChangeListener((preference, newValue) -> {
            String newUUID = newValue.toString();
            if (!SponsorBlockSettings.isValidSBUserId(newUUID)) {
                Utils.showToastLong(str("revanced_sb_general_uuid_invalid"));
                return false;
            }

            Settings.SB_PRIVATE_USER_ID.save(newUUID);
            try {
                updateUI();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            fetchAndDisplayStats();
            return true;
        });
        final Preference apiUrl = mPreferenceManager.findPreference(Settings.SB_API_URL.key);
        if (apiUrl != null) {
            apiUrl.setOnPreferenceClickListener(preference -> {
                Context context = preference.getContext();

                TableLayout table = new TableLayout(context);
                table.setOrientation(LinearLayout.HORIZONTAL);
                table.setPadding(15, 0, 15, 0);

                TableRow row = new TableRow(context);

                EditText editText = new EditText(context);
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                editText.setText(Settings.SB_API_URL.get());
                editText.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(editText);
                table.addView(row);

                DialogInterface.OnClickListener urlChangeListener = (dialog, buttonPressed) -> {
                    if (buttonPressed == DialogInterface.BUTTON_NEUTRAL) {
                        Settings.SB_API_URL.resetToDefault();
                        Utils.showToastLong(str("revanced_sb_api_url_reset"));
                    } else if (buttonPressed == DialogInterface.BUTTON_POSITIVE) {
                        String serverAddress = editText.getText().toString();
                        if (!SponsorBlockSettings.isValidSBServerAddress(serverAddress)) {
                            Utils.showToastLong(str("revanced_sb_api_url_invalid"));
                        } else if (!serverAddress.equals(Settings.SB_API_URL.get())) {
                            Settings.SB_API_URL.save(serverAddress);
                            Utils.showToastLong(str("revanced_sb_api_url_changed"));
                        }
                    }
                };
                Utils.getEditTextDialogBuilder(context)
                        .setView(table)
                        .setTitle(apiUrl.getTitle())
                        .setNegativeButton(android.R.string.cancel, null)
                        .setNeutralButton(str("revanced_sb_reset"), urlChangeListener)
                        .setPositiveButton(android.R.string.ok, urlChangeListener)
                        .show();
                return true;
            });
        }

        statsCategory = new PreferenceCategory(mActivity);
        statsCategory.setLayoutResource(preferencesCategoryLayout);
        statsCategory.setTitle(str("revanced_sb_stats"));
        mPreferenceScreen.addPreference(statsCategory);
        fetchAndDisplayStats();

        final PreferenceCategory aboutCategory = new PreferenceCategory(mActivity);
        aboutCategory.setLayoutResource(preferencesCategoryLayout);
        aboutCategory.setTitle(str("revanced_sb_about"));
        mPreferenceScreen.addPreference(aboutCategory);

        Preference aboutPreference = new Preference(mActivity);
        aboutCategory.addPreference(aboutPreference);
        aboutPreference.setTitle(str("revanced_sb_about_api"));
        aboutPreference.setSummary(str("revanced_sb_about_api_sum"));
        aboutPreference.setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://sponsor.ajay.app"));
            preference.getContext().startActivity(i);
            return false;
        });

        updateUI();
    }

    public static void updateSegmentCategories() {
        try {
            for (SegmentCategory category : SegmentCategory.categoriesWithoutUnsubmitted()) {
                final String key = category.keyValue;
                if (mPreferenceManager.findPreference(key) instanceof SegmentCategoryListPreference segmentCategoryListPreference) {
                    segmentCategoryListPreference.setTitle(category.getTitleWithColorDot());
                    segmentCategoryListPreference.setEnabled(Settings.SB_ENABLED.get());
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "updateSegmentCategories failure", ex);
        }
    }

    private static void openGuidelines(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://wiki.sponsor.ajay.app/w/Guidelines"));
        context.startActivity(intent);
    }

    public static void fetchAndDisplayStats() {
        try {
            if (statsCategory == null) {
                return;
            }
            statsCategory.removeAll();
            if (!SponsorBlockSettings.userHasSBPrivateId()) {
                // User has never voted or created any segments.  No stats to show.
                addLocalUserStats();
                return;
            }

            Context context = statsCategory.getContext();

            Preference loadingPlaceholderPreference = new Preference(context);
            loadingPlaceholderPreference.setEnabled(false);
            statsCategory.addPreference(loadingPlaceholderPreference);
            if (Settings.SB_ENABLED.get()) {
                loadingPlaceholderPreference.setTitle(str("revanced_sb_stats_loading"));
                Utils.runOnBackgroundThread(() -> {
                    UserStats stats = SBRequester.retrieveUserStats();
                    Utils.runOnMainThread(() -> { // get back on main thread to modify UI elements
                        addUserStats(loadingPlaceholderPreference, stats);
                        addLocalUserStats();
                    });
                });
            } else {
                loadingPlaceholderPreference.setTitle(str("revanced_sb_stats_sb_disabled"));
            }
        } catch (Exception ex) {
            Logger.printException(() -> "fetchAndDisplayStats failure", ex);
        }
    }

    private static void addUserStats(@NonNull Preference loadingPlaceholder, @Nullable UserStats stats) {
        Utils.verifyOnMainThread();
        try {
            if (stats == null) {
                loadingPlaceholder.setTitle(str("revanced_sb_stats_connection_failure"));
                return;
            }
            statsCategory.removeAll();
            Context context = statsCategory.getContext();

            if (stats.totalSegmentCountIncludingIgnored > 0) {
                // If user has not created any segments, there's no reason to set a username.
                ResettableEditTextPreference preference = new ResettableEditTextPreference(context);
                statsCategory.addPreference(preference);
                String userName = stats.userName;
                preference.setTitle(fromHtml(str("revanced_sb_stats_username", userName)));
                preference.setSummary(str("revanced_sb_stats_username_change"));
                preference.setText(userName);
                preference.setOnPreferenceChangeListener((preference1, value) -> {
                    Utils.runOnBackgroundThread(() -> {
                        String newUserName = (String) value;
                        String errorMessage = SBRequester.setUsername(newUserName);
                        Utils.runOnMainThread(() -> {
                            if (errorMessage == null) {
                                preference.setTitle(fromHtml(str("revanced_sb_stats_username", newUserName)));
                                preference.setText(newUserName);
                                Utils.showToastLong(str("revanced_sb_stats_username_changed"));
                            } else {
                                preference.setText(userName); // revert to previous
                                SponsorBlockUtils.showErrorDialog(errorMessage);
                            }
                        });
                    });
                    return true;
                });
            }

            {
                // number of segment submissions (does not include ignored segments)
                Preference preference = new Preference(context);
                statsCategory.addPreference(preference);
                String formatted = SponsorBlockUtils.getNumberOfSkipsString(stats.segmentCount);
                preference.setTitle(fromHtml(str("revanced_sb_stats_submissions", formatted)));
                preference.setSummary(str("revanced_sb_stats_submissions_sum"));
                if (stats.totalSegmentCountIncludingIgnored == 0) {
                    preference.setSelectable(false);
                } else {
                    preference.setOnPreferenceClickListener(preference1 -> {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("https://sb.ltn.fi/userid/" + stats.publicUserId));
                        preference1.getContext().startActivity(i);
                        return true;
                    });
                }
            }

            {
                // "user reputation".  Usually not useful, since it appears most users have zero reputation.
                // But if there is a reputation, then show it here
                Preference preference = new Preference(context);
                preference.setTitle(fromHtml(str("revanced_sb_stats_reputation", stats.reputation)));
                preference.setSelectable(false);
                if (stats.reputation != 0) {
                    statsCategory.addPreference(preference);
                }
            }

            {
                // time saved for other users
                Preference preference = new Preference(context);
                statsCategory.addPreference(preference);

                String stats_saved;
                String stats_saved_sum;
                if (stats.totalSegmentCountIncludingIgnored == 0) {
                    stats_saved = str("revanced_sb_stats_saved_zero");
                    stats_saved_sum = str("revanced_sb_stats_saved_sum_zero");
                } else {
                    stats_saved = str("revanced_sb_stats_saved",
                            SponsorBlockUtils.getNumberOfSkipsString(stats.viewCount));
                    stats_saved_sum = str("revanced_sb_stats_saved_sum", SponsorBlockUtils.getTimeSavedString((long) (60 * stats.minutesSaved)));
                }
                preference.setTitle(fromHtml(stats_saved));
                preference.setSummary(fromHtml(stats_saved_sum));
                preference.setOnPreferenceClickListener(preference1 -> {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://sponsor.ajay.app/stats/"));
                    preference1.getContext().startActivity(i);
                    return false;
                });
            }
        } catch (Exception ex) {
            Logger.printException(() -> "addUserStats failure", ex);
        }
    }

    private static void addLocalUserStats() {
        // time the user saved by using SB
        Preference preference = new Preference(statsCategory.getContext());
        statsCategory.addPreference(preference);

        Runnable updateStatsSelfSaved = () -> {
            String formatted = SponsorBlockUtils.getNumberOfSkipsString(Settings.SB_LOCAL_TIME_SAVED_NUMBER_SEGMENTS.get());
            preference.setTitle(fromHtml(str("revanced_sb_stats_self_saved", formatted)));
            String formattedSaved = SponsorBlockUtils.getTimeSavedString(Settings.SB_LOCAL_TIME_SAVED_MILLISECONDS.get() / 1000);
            preference.setSummary(fromHtml(str("revanced_sb_stats_self_saved_sum", formattedSaved)));
        };
        updateStatsSelfSaved.run();
        preference.setOnPreferenceClickListener(preference1 -> {
            new AlertDialog.Builder(preference1.getContext())
                    .setTitle(str("revanced_sb_stats_self_saved_reset_title"))
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        Settings.SB_LOCAL_TIME_SAVED_NUMBER_SEGMENTS.resetToDefault();
                        Settings.SB_LOCAL_TIME_SAVED_MILLISECONDS.resetToDefault();
                        updateStatsSelfSaved.run();
                    })
                    .setNegativeButton(android.R.string.no, null).show();
            return true;
        });
    }
}
