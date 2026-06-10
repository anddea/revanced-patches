/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - Jav1x (https://github.com/Jav1x)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Attribution Notice
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Attribution (Section 7(b)): This specific copyright notice and the
 *    list of original authors above must be preserved in any copy or
 *    derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin (Section 7(c)): Modified versions must be clearly marked as
 *    such (e.g., by adding a "Modified by" line or a new copyright notice).
 *    They must not be misrepresented as the original work.
 *
 * ------------------------------------------------------------------------
 * Version Control Acknowledgement (Non-binding Request)
 * ------------------------------------------------------------------------
 *
 * While not a legal requirement of the GPLv3, the original author(s)
 * respectfully request that ports or substantial modifications retain
 * historical authorship credit in version control systems (e.g., Git),
 * listing original author(s) appropriately and modifiers as committers
 * or co-authors.
 */

package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.Preference;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.youtube.patches.voiceovertranslation.VotApiClient;
import app.morphe.extension.youtube.patches.voiceovertranslation.VotAuthWebViewDialog;
import app.morphe.extension.youtube.settings.Settings;

/**
 * Custom preference for managing the Yandex OAuth token used by Voice Over Translation.
 * <p>
 * When the user is not signed in, clicking shows a dialog with two options:
 * "Sign in with Yandex" (opens a WebView OAuth flow) or "Enter token manually".
 * When signed in, clicking shows account info with options to sign out or switch token.
 */
@SuppressWarnings({"deprecation", "unused"})
public class VotOAuthPreference extends Preference implements Preference.OnPreferenceClickListener {

    /** Cached profile display name (static to survive preference recreation). */
    @Nullable
    private static String cachedDisplayName;

    /** Cached avatar URL (static to survive preference recreation). */
    @Nullable
    private static String cachedAvatarUrl;

    /** Cached avatar bitmap (static to survive preference recreation). */
    @Nullable
    private static Bitmap cachedAvatarBitmap;

    /** Timeout values for profile fetch. */
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 10000;

    {
        setOnPreferenceClickListener(this);
    }

    //region Constructors ----------------------------------------------------------------

    public VotOAuthPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public VotOAuthPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public VotOAuthPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VotOAuthPreference(Context context) {
        super(context);
        init();
    }

    //endregion

    private void init() {
        updateUI();
    }

    @Override
    protected void onBindView(android.view.View view) {
        updateUI();
        super.onBindView(view);
    }

    //region UI update ------------------------------------------------------------------

    /**
     * Returns {@code true} if a non-empty OAuth token is currently saved.
     */
    private boolean isSignedIn() {
        String token = Settings.VOT_OAUTH_TOKEN.get();
        return !token.isEmpty();
    }

    /**
     * Updates the summary text and appearance based on sign-in state.
     */
    void updateUI() {
        if (isSignedIn()) {
            long expiresAt = Settings.VOT_OAUTH_TOKEN_EXPIRES_AT.get();
            boolean expired = expiresAt > 0 && System.currentTimeMillis() > expiresAt;

            if (expired) {
                setSummary(str("revanced_vot_oauth_expired_summary"));
                return;
            }

            String name = cachedDisplayName != null && !cachedDisplayName.isEmpty()
                    ? cachedDisplayName
                    : "Yandex";

            if (expiresAt > 0) {
                long remainingMs = expiresAt - System.currentTimeMillis();
                int daysRemaining = (int) Math.max(1, (remainingMs + 86_399_999L) / 86_400_000L);
                setSummary(str("revanced_vot_oauth_signed_in_expires", name, daysRemaining));
            } else if (cachedDisplayName != null && !cachedDisplayName.isEmpty()) {
                setSummary(str("revanced_vot_oauth_signed_in_summary", cachedDisplayName));
            } else {
                setSummary(str("revanced_vot_oauth_signed_in_summary", "Yandex"));
                // Try to load profile info in the background
                loadProfileAsync();
            }
        } else {
            cachedDisplayName = null;
            setSummary(str("revanced_vot_oauth_not_signed_in_summary"));
        }
    }

    //endregion

    //region Click handling -------------------------------------------------------------

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (isSignedIn()) {
            showAccountManagementDialog();
        } else {
            showAuthMethodDialog();
        }
        return true;
    }

    //endregion

    //region Dialogs --------------------------------------------------------------------

    /**
     * Dialog shown when the user is NOT signed in.
     * Offers two options: "Sign in with Yandex" (WebView) or "Enter token manually".
     */
    private void showAuthMethodDialog() {
        Context context = getContext();

        CustomDialog.create(
                context,
                str("revanced_vot_oauth_auth_method_title"),
                str("revanced_vot_oauth_auth_method_message"),
                null,
                str("revanced_vot_oauth_sign_in_button"),
                () -> openWebViewAuth(context),
                () -> { /* Cancel — do nothing */ },
                str("revanced_vot_oauth_enter_manually_button"),
                () -> showManualTokenDialog(context),
                false
        ).first.show();
    }

    /**
     * Dialog shown when the user IS signed in.
     * Shows account info and offers "Sign out" or "Use another token".
     */
    private void showAccountManagementDialog() {
        Context context = getContext();

        String displayName = cachedDisplayName != null
                ? cachedDisplayName
                : "Yandex";

        // Pass null as message — we add our own account layout below.
        Pair<Dialog, LinearLayout> pair = CustomDialog.create(
                context,
                str("revanced_vot_oauth_account_management_title"),
                null,
                null,
                str("revanced_vot_oauth_sign_out_button"),
                this::signOut,
                () -> { /* Cancel — do nothing */ },
                str("revanced_vot_oauth_switch_token_button"),
                this::showAuthMethodDialog,
                false
        );
        Dialog dialog = pair.first;
        LinearLayout mainLayout = pair.second;

        // Build horizontal account row: circular avatar (left) + display name (right)
        LinearLayout accountLayout = new LinearLayout(context);
        accountLayout.setOrientation(LinearLayout.HORIZONTAL);
        accountLayout.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams accountParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        accountParams.setMargins(0, Utils.dipToPixels(8), 0, Utils.dipToPixels(12));
        accountLayout.setLayoutParams(accountParams);

        boolean hasAvatar = cachedAvatarUrl != null && !cachedAvatarUrl.isEmpty();

        // Circular avatar on the left
        if (hasAvatar) {
            int avatarSize = Utils.dipToPixels(48);
            ImageView avatarView = new ImageView(context);
            avatarView.setLayoutParams(new LinearLayout.LayoutParams(avatarSize, avatarSize));
            avatarView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View view, android.graphics.Outline outline) {
                    int s = Math.min(view.getWidth(), view.getHeight());
                    outline.setOval(0, 0, s, s);
                }
            });
            avatarView.setClipToOutline(true);
            avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            if (cachedAvatarBitmap != null) {
                avatarView.setImageBitmap(cachedAvatarBitmap);
            } else {
                loadAvatarAsync(avatarView);
            }
            accountLayout.addView(avatarView);
        }

        // Display name on the right
        TextView nameView = new TextView(context);
        nameView.setText(displayName);
        nameView.setTextSize(16);
        nameView.setTextColor(BaseThemeUtils.getAppForegroundColor());
        LinearLayout.LayoutParams nameParams = hasAvatar
                ? new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                : new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        if (hasAvatar) {
            nameParams.setMargins(Utils.dipToPixels(16), 0, 0, 0);
        }
        nameView.setLayoutParams(nameParams);
        accountLayout.addView(nameView);

        // Insert at position 1 (after title, before buttons)
        mainLayout.addView(accountLayout, 1);

        dialog.show();
    }

    /**
     * Shows the manual token entry dialog with an EditText.
     */
    private void showManualTokenDialog(Context context) {
        EditText editText = new EditText(context);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setHint("y0_AgAAAAB...");
        // Pre-fill with current token if any
        String currentToken = Settings.VOT_OAUTH_TOKEN.get();
        if (!currentToken.isEmpty()) {
            editText.setText(currentToken);
            editText.setSelection(currentToken.length());
        }

        CustomDialog.create(
                context,
                str("revanced_vot_oauth_enter_token_title"),
                null,
                editText,
                null,
                () -> {
                    String token = editText.getText().toString().trim();
                    if (token.isEmpty()) {
                        return;
                    }
                    onTokenObtained(token, 0);
                },
                () -> { /* Cancel — do nothing */ },
                null,
                null,
                true
        ).first.show();
    }

    //endregion

    //region WebView OAuth flow ---------------------------------------------------------

    /**
     * Opens the fullscreen WebView dialog for the Yandex OAuth flow.
     */
    private void openWebViewAuth(Context context) {
        try {
            VotAuthWebViewDialog dialog = new VotAuthWebViewDialog(context,
                    new VotAuthWebViewDialog.OnTokenReceivedListener() {
                        @Override
                        public void onTokenReceived(@NonNull String token, long expiresIn) {
                            onTokenObtained(token, expiresIn);
                        }

                        @Override
                        public void onCancelled() {
                            Logger.printDebug(() -> "VotOAuthPreference: WebView auth cancelled");
                        }
                    });
            dialog.show();
        } catch (Exception e) {
            Logger.printException(() -> "VotOAuthPreference: failed to open WebView", e);
            Utils.showToastLong(str("revanced_vot_oauth_no_network"));
        }
    }

    //endregion

    //region Token handling -------------------------------------------------------------

    /**
     * Called when a token is obtained (from WebView or manual entry).
     * Validates the token, loads profile info, saves it, and updates UI.
     */
    private void onTokenObtained(@NonNull String token, long expiresIn) {
        Utils.runOnBackgroundThread(() -> {
            // Validate the token first
            if (!VotApiClient.isValidOAuthToken(token)) {
                Utils.runOnMainThread(() -> Utils.showToastLong(str("revanced_vot_oauth_invalid_token")));
                return;
            }

            // Save the token
            Settings.VOT_OAUTH_TOKEN.save(token);

            // Save expiry timestamp
            if (expiresIn > 0) {
                long expiresAt = System.currentTimeMillis() + Math.max(0, expiresIn - 60) * 1000L;
                Settings.VOT_OAUTH_TOKEN_EXPIRES_AT.save(expiresAt);
            } else {
                Settings.VOT_OAUTH_TOKEN_EXPIRES_AT.save(0L);
            }

            // Load profile info
            String displayName = fetchDisplayName(token);

            Utils.runOnMainThread(() -> {
                cachedDisplayName = displayName;
                updateUI();
            });
        });
    }

    /**
     * Signs out: clears the token and cached profile, resets validation cache.
     */
    private void signOut() {
        Settings.VOT_OAUTH_TOKEN.save("");
        Settings.VOT_OAUTH_TOKEN_EXPIRES_AT.save(0L);
        VotApiClient.clearTokenValidationCache();
        cachedDisplayName = null;
        cachedAvatarUrl = null;
        cachedAvatarBitmap = null;
        updateUI();
    }

    //endregion

    //region Profile loading ------------------------------------------------------------

    /**
     * Loads the user's display name from Yandex Passport API asynchronously.
     * Updates the UI when done.
     */
    private void loadProfileAsync() {
        String token = Settings.VOT_OAUTH_TOKEN.get();
        if (token.isEmpty()) return;

        Utils.runOnBackgroundThread(() -> {
            String displayName = fetchDisplayName(token);
            if (displayName != null) {
                Utils.runOnMainThread(() -> {
                    cachedDisplayName = displayName;
                    updateUI();
                });
            }
        });
    }

    /**
     * Fetches the display name and avatar URL for a Yandex OAuth token.
     *
     * @param token valid OAuth token
     * @return display name, or {@code null} on failure
     */
    @Nullable
    private static String fetchDisplayName(@NonNull String token) {
        try {
            String url = "https://login.yandex.ru/info?format=json";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            try {
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "OAuth " + token);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);

                int code = conn.getResponseCode();
                if (code != 200) {
                    Logger.printDebug(() -> "VotOAuthPreference: profile fetch returned HTTP " + code);
                    return null;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }

                JSONObject json = new JSONObject(sb.toString());

                // Parse avatar
                if (!json.optBoolean("is_avatar_empty", true)) {
                    String avatarId = json.optString("default_avatar_id", "");
                    if (!avatarId.isEmpty()) {
                        cachedAvatarUrl = "https://avatars.yandex.net/get-yapic/"
                                + avatarId + "/islands-200";
                    } else {
                        cachedAvatarUrl = null;
                    }
                } else {
                    cachedAvatarUrl = null;
                }

                String displayName = json.optString("login", "");
                if (displayName.isEmpty()) {
                    displayName = json.optString("display_name", "");
                }
                if (displayName.isEmpty()) {
                    displayName = json.optString("real_name", "");
                }
                final String finalDisplayName = displayName;
                final String finalAvatarUrl = cachedAvatarUrl;
                Logger.printDebug(() -> "VotOAuthPreference: fetched login="
                        + finalDisplayName + " avatar=" + finalAvatarUrl);
                return finalDisplayName;

            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            Logger.printException(() -> "VotOAuthPreference: profile fetch failed", e);
            return null;
        }
    }

    /**
     * Loads the Yandex avatar bitmap from {@link #cachedAvatarUrl} on a background thread
     * and sets it on the given ImageView on the main thread.
     */
    private static void loadAvatarAsync(@NonNull ImageView avatarView) {
        final String avatarUrl = cachedAvatarUrl;
        if (avatarUrl == null || avatarUrl.isEmpty()) return;

        final WeakReference<ImageView> avatarRef = new WeakReference<>(avatarView);

        Utils.runOnBackgroundThread(() -> {
            try {
                URL url = new URL(avatarUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                try {
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                    conn.setReadTimeout(READ_TIMEOUT_MS);

                    if (conn.getResponseCode() != 200) return;

                    InputStream is = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    is.close();

                    if (bitmap != null) {
                        cachedAvatarBitmap = bitmap;
                        Utils.runOnMainThread(() -> {
                            ImageView target = avatarRef.get();
                            if (target != null) {
                                target.setImageBitmap(bitmap);
                            }
                        });
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                Logger.printDebug(() -> "VotOAuthPreference: avatar load failed: " + e.getMessage());
            }
        });
    }

    //endregion
}
