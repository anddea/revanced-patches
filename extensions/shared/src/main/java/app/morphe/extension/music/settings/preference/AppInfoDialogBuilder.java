/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.music.settings.preference;

import static app.morphe.extension.shared.patches.PatchStatus.PatchVersion;
import static app.morphe.extension.shared.patches.PatchStatus.PatchedTime;
import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;

import java.util.Date;
import java.util.Locale;

import app.morphe.extension.shared.settings.preference.WebViewDialog;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.PackageUtils;
import app.morphe.extension.shared.utils.Utils;

/**
 * Used by YouTube and YouTube Music.
 */
public class AppInfoDialogBuilder {

    public static void showDialog(Activity mActivity) {
        try {
            final String backgroundColorHex = BaseThemeUtils.getBackgroundColorHexString();
            final String foregroundColorHex = BaseThemeUtils.getForegroundColorHexString();

            long patchedTime = PatchedTime();
            Date date = new Date(patchedTime);

            final String creditsUrl = "https://github.com/anddea/revanced-patches/wiki/Credits";

            final String htmlDialog = "<html>" +
                    "<body style=\"padding: 15px;\"><p>" +
                    String.format(
                            Locale.ENGLISH,
                            "<style> body { background-color: %s; color: %s; line-height: 20px; } a { color: %s; text-decoration: underline; } </style>",
                            backgroundColorHex, foregroundColorHex, foregroundColorHex) +
                    "<h2>" +
                    str("revanced_app_info_dialog_title") +
                    "</h2>" +
                    String.format(
                            str("revanced_app_info_dialog_message"),
                            PackageUtils.getAppLabel(),
                            PackageUtils.getAppVersionName(),
                            PatchVersion(),
                            date.toLocaleString()
                    ) +
                    "<br><br>" +
                    "<a href=\"" + creditsUrl + "\">" + str("revanced_credits_title") + "</a>" +
                    "</p></body></html>";

            Utils.runOnMainThreadNowOrLater(() -> {
                WebViewDialog webViewDialog = new WebViewDialog(mActivity, htmlDialog);
                webViewDialog.show();
            });
        } catch (Exception ex) {
            Logger.printException(() -> "dialogBuilder failure", ex);
        }
    }
}
