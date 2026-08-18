/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
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

package app.morphe.extension.music.patches.utils;

import static app.morphe.extension.shared.utils.Utils.isSDKAbove;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;

@SuppressWarnings({"unused", "deprecation"})
public class DrawableColorPatch {
    public static final String DEFAULT_DARK_THEME = "modern_youtube";
    public static final String DEFAULT_DARK_THEME_CUSTOM_COLOR = "#FF000000";

    private static final int PRECOMPILED_THEME_QUALIFIER_BASE = 801;
    private static final String[] PRECOMPILED_DARK_THEME_KEYS = {
            "stock", "amoled_black", "material_you_neutral", "material_you_primary",
            "material_you_secondary", "material_you_tertiary", "modern_youtube",
            "classic_youtube", "catppuccin_mocha", "dark_pink", "dark_blue", "dark_green",
            "dark_yellow", "dark_orange", "dark_red",
    };
    private static final int[] DARK_COLORS = {
            0xFF212121, // comments box background
            0xFF030303, // button container background in album
            0xFF000000, // button container background in playlist
    };

    private static final int STOCK_DARK_THEME_MAIN_COLOR_INDEX = 5;
    private static final int[] STOCK_DARK_THEME_COLORS = {
            0xFF282828, // yt_black0
            0xFF212121, // yt_black1
            0xF2212121, // yt_black1_opacity95
            0xFA212121, // yt_black1_opacity98
            0xFF181818, // yt_black2
            0xFF0F0F0F, // yt_black3
            0xFF030303, // yt_black4
            0xFF000000, // yt_black_pure
            0xCC000000, // yt_black_pure_opacity80
            0xFF131313, // yt_status_bar_background_dark
            0xFF1D1D1D, // ytm_color_grey_12
            0xFF424242, // material_grey_800
            0xFF303030, // material_grey_850
    };

    private static final Drawable transparentDrawable =
            new ColorDrawable(Color.TRANSPARENT);

    /**
     * Installs the selected dark palette before the activity inflates its first layout. Android
     * 8–10 select precompiled resource configurations, while Android 11+ replaces the stable
     * resources.
     */
    public static void setTheme(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                PrecompiledResourcePalette.install(activity);
            } catch (Exception ex) {
                Logger.printException(() -> "Failed to apply precompiled dark theme", ex);
            }
            return;
        }

        try {
            int[] colors = getSelectedColors(activity);
            BaseThemeUtils.setThemeDarkColor(colors[STOCK_DARK_THEME_MAIN_COLOR_INDEX]);
            RuntimeResourceOverlay.install(activity, colors);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to install runtime dark theme", ex);
        }
    }

    public static int getLithoColor(int colorValue) {
        if (!ArrayUtils.contains(DARK_COLORS, colorValue)) return colorValue;
        return isStockTheme() ? colorValue : BaseThemeUtils.getThemeDarkColor();
    }

    public static void setHeaderGradient(ViewGroup viewGroup) {
        viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!(viewGroup.getChildAt(0) instanceof ViewGroup firstChildView))
                return;
            View secondChildView = firstChildView.getChildAt(0);

            if (secondChildView instanceof ImageView gradientView) {
                // Album
                setHeaderGradient(gradientView);
            } else if (secondChildView instanceof ViewGroup thirdChildView &&
                    thirdChildView.getChildCount() == 1 &&
                    thirdChildView.getChildAt(0) instanceof ImageView gradientView) {
                // Playlist
                setHeaderGradient(gradientView);
            }
        });
    }

    private static void setHeaderGradient(ImageView gradientView) {
        final Drawable headerGradient = ResourceUtils.getDrawable("revanced_header_gradient");
        // headerGradient is litho, so this view is sometimes used elsewhere, like the button of the action bar.
        // In order to prevent the gradient to be applied to the button of the action bar,
        // Add a layout listener to the ImageView.
        if (isSDKAbove(23) && headerGradient != null && gradientView.getForeground() == null) {
            gradientView.setForeground(headerGradient);
            gradientView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                if (gradientView.getParent() instanceof View view &&
                        view.getContentDescription() != null &&
                        gradientView.getForeground() == headerGradient
                ) {
                    gradientView.setForeground(transparentDrawable);
                }
            });
        }
    }

    private static boolean isStockTheme() {
        return "stock".equals(Settings.DARK_THEME.get());
    }

    private static int[] getSelectedColors(Context context) {
        if (isStockTheme()) return STOCK_DARK_THEME_COLORS.clone();

        int[] colors = new int[STOCK_DARK_THEME_COLORS.length];
        int selectedColor = getSelectedColor(context);
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.argb(
                    Color.alpha(STOCK_DARK_THEME_COLORS[i]),
                    Color.red(selectedColor),
                    Color.green(selectedColor),
                    Color.blue(selectedColor)
            );
        }
        return colors;
    }

    private static int getSelectedColor(Context context) {
        final int patchedColor = STOCK_DARK_THEME_COLORS[STOCK_DARK_THEME_MAIN_COLOR_INDEX];
        return switch (Settings.DARK_THEME.get()) {
            case "amoled_black" -> Color.BLACK;
            case "material_you_neutral" -> getSystemColor(context, "system_neutral1_900", patchedColor);
            case "material_you_primary" -> getSystemColor(context, "system_accent1_800", patchedColor);
            case "material_you_secondary" -> getSystemColor(context, "system_accent2_800", patchedColor);
            case "material_you_tertiary" -> getSystemColor(context, "system_accent3_800", patchedColor);
            case "modern_youtube" -> 0xFF0F0F0F;
            case "classic_youtube" -> 0xFF212121;
            case "catppuccin_mocha" -> 0xFF181825;
            case "dark_pink" -> 0xFF290025;
            case "dark_blue" -> 0xFF001029;
            case "dark_green" -> 0xFF002905;
            case "dark_yellow" -> 0xFF282900;
            case "dark_orange" -> 0xFF291800;
            case "dark_red" -> 0xFF290000;
            case "custom" -> ResourceUtils.getColor(Settings.DARK_THEME_CUSTOM_COLOR.get(), patchedColor);
            default -> patchedColor;
        };
    }

    @SuppressLint("DiscouragedApi")
    private static int getSystemColor(Context context, String name, int fallback) {
        int identifier = Resources.getSystem().getIdentifier(name, "color", "android");
        if (identifier == 0) return fallback;
        try {
            return context.getResources().getColor(identifier, context.getTheme());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * Selects concrete resource variants using a synthetic MCC qualifier. This changes only the
     * process-local resource configuration and avoids unsupported AssetManager APIs.
     */
    private static final class PrecompiledResourcePalette {
        private PrecompiledResourcePalette() {
        }

        static synchronized void install(Activity activity) {
            Resources activityResources = activity.getResources();
            int themedMcc = qualifierFor(Settings.DARK_THEME.get());
            apply(activityResources, themedMcc);

            Resources applicationResources = activity.getApplicationContext().getResources();
            if (applicationResources != activityResources) {
                apply(applicationResources, themedMcc);
            }

            // Refresh extension and Litho colors from the now-selected concrete resources.
            BaseThemeUtils.setThemeColor();
        }

        private static void apply(Resources resources, int themedMcc) {
            Configuration current = resources.getConfiguration();
            if (current.mcc == themedMcc) return;

            Configuration themed = new Configuration(current);
            themed.mcc = themedMcc;
            resources.updateConfiguration(themed, resources.getDisplayMetrics());
        }

        private static int qualifierFor(String selection) {
            int defaultIndex = 0;
            for (int i = 0; i < PRECOMPILED_DARK_THEME_KEYS.length; i++) {
                if (PRECOMPILED_DARK_THEME_KEYS[i].equals(selection)) {
                    return PRECOMPILED_THEME_QUALIFIER_BASE + i;
                }
                if (PRECOMPILED_DARK_THEME_KEYS[i].equals(DEFAULT_DARK_THEME)) {
                    defaultIndex = i;
                }
            }
            return PRECOMPILED_THEME_QUALIFIER_BASE + defaultIndex;
        }
    }

    /** API 30 classes stay isolated so this patch remains verifier-safe on Android 8–10. */
    private static final class RuntimeResourceOverlay {
        private static final byte[][] DARK_PLACEHOLDERS = {
                {0x50, 0x34, 0x12, (byte) 0xFF}, {0x51, 0x34, 0x12, (byte) 0xFF},
                {0x52, 0x34, 0x12, (byte) 0xFF}, {0x53, 0x34, 0x12, (byte) 0xFF},
                {0x54, 0x34, 0x12, (byte) 0xFF}, {0x55, 0x34, 0x12, (byte) 0xFF},
                {0x56, 0x34, 0x12, (byte) 0xFF}, {0x57, 0x34, 0x12, (byte) 0xFF},
                {0x58, 0x34, 0x12, (byte) 0xFF}, {0x59, 0x34, 0x12, (byte) 0xFF},
                {0x5A, 0x34, 0x12, (byte) 0xFF}, {0x5B, 0x34, 0x12, (byte) 0xFF},
                {0x5C, 0x34, 0x12, (byte) 0xFF},
        };
        private static final String COMPRESSED_TABLE =
                "H4sIAGSJhWoAA+3dz0/TcBjH8adFFBQFPHkgZkYOBpKlwEBIMJKYeBV1/v5Ry2jGQrcupTPhovwBHryYeDTx4tGjRxP+Af8ETp71L8Cn2wMsQVIXPBjzfpEP3366p+vSbMemrgzJzisRR7KM6V8X3bnf3YLzRede62ZFYqlLUaq6xvo/klBbIA1ZlUT31HQtyqZutSTVrOjr/5OC01nfOAf7smsX6Xqya67UtT2gGdFcyI7XXNG4cl2mbJ22dcbWkq2zuva3r3ikSfbP5fWJjB6cXF50neuy5qZmQ/NBs635rhnUuSVNQ/NOs635qbnoitzSbGk+agqFepw010I/aTXSWj30V4Nk3U/XQt1ciYLKupc/MiUTE3kjftwMKrV0c2G2l+H5/JNP54/M5I+UpFQ6emQjDdLWhr8SJJrKejWJW43V9oBMTh59WD1Iw6QWRH41CTf9+VlPxsdzPoffbCVhL2/qeT1ML+h07tX39q7+nCfF4p983r0D5ns9YO43X66oVl1L7YhKHMXJ4U98aGTv/a564rojUl7MfpuOjA4f77cPAAAAAAAAAAAAAAAAAAAAAAAAAAAA4K9bOm4ctyyfrnXu98/uKX6/2HnhOHYBAAAAAAAAAAAAAAAAAAAAAAAAAAAA/DOye4CzZ4QXNJ7daLwsnWd7NzVbmrfSeab3Z81XzTfNjuZHdrCTPWfcbd9LPCAytlw6v5t1x/pt6671O9b7rN+1fsJ62Xq/9XvWT1q/b/2U9QfWB6w/tD5o/ZH109YfWz9j/Yn1IetPrZ+1/sz6OevPrQ9bv3QjbPcR69Pll7u/AI6KtVjgfgAA";

        private static ResourcesLoader loader;

        private RuntimeResourceOverlay() {
        }

        @RequiresApi(api = Build.VERSION_CODES.R)
        static synchronized void install(Activity activity, int[] colors) throws IOException {
            if (loader != null) return;

            byte[] table = inflateTable();
            for (int i = 0; i < DARK_PLACEHOLDERS.length; i++) {
                replacePlaceholderColor(table, DARK_PLACEHOLDERS[i], colors[i]);
            }
            File tableFile = new File(activity.getCacheDir(), "morphe-dark-theme.arsc");
            try (FileOutputStream output = new FileOutputStream(tableFile, false)) {
                output.write(table);
            }

            ResourcesProvider provider;
            try (ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(
                    tableFile, ParcelFileDescriptor.MODE_READ_ONLY)) {
                provider = ResourcesProvider.loadFromTable(descriptor, null);
            }
            loader = new ResourcesLoader();
            loader.addProvider(provider);

            Resources activityResources = activity.getResources();
            activityResources.addLoaders(loader);
            Resources applicationResources = activity.getApplicationContext().getResources();
            if (applicationResources != activityResources) {
                applicationResources.addLoaders(loader);
            }
        }

        private static byte[] inflateTable() throws IOException {
            byte[] compressed = Base64.decode(COMPRESSED_TABLE, Base64.DEFAULT);
            try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed));
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                return output.toByteArray();
            }
        }

        private static void replacePlaceholderColor(byte[] table, byte[] placeholder, int color)
                throws IOException {
            for (int i = 0; i <= table.length - placeholder.length; i++) {
                if (table[i] == placeholder[0]
                        && table[i + 1] == placeholder[1]
                        && table[i + 2] == placeholder[2]
                        && table[i + 3] == placeholder[3]) {
                    table[i] = (byte) color;
                    table[i + 1] = (byte) (color >>> 8);
                    table[i + 2] = (byte) (color >>> 16);
                    table[i + 3] = (byte) (color >>> 24);
                    return;
                }
            }
            throw new IOException("Runtime theme placeholder not found");
        }
    }
}
