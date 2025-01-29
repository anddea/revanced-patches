package app.revanced.extension.music.patches.utils;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.apache.commons.lang3.ArrayUtils;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;

@SuppressWarnings("unused")
public class DrawableColorPatch {
    private static final int[] DARK_COLORS = {
            0xFF212121, // comments box background
            0xFF030303, // button container background in album
            0xFF000000, // button container background in playlist
    };

    // background colors
    private static final Drawable headerGradient =
            ResourceUtils.getDrawable("revanced_header_gradient");
    private static final int blackColor =
            ResourceUtils.getColor("yt_black1");
    private static final int elementsContainerIdentifier =
            ResourceUtils.getIdIdentifier("elements_container");

    public static int getLithoColor(int colorValue) {
        return ArrayUtils.contains(DARK_COLORS, colorValue)
                ? blackColor
                : colorValue;
    }

    public static void setHeaderGradient(ViewGroup viewGroup) {
        viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!(viewGroup.getChildAt(0) instanceof ViewGroup firstChildView))
                return;
            View secondChildView = firstChildView.getChildAt(0);

            if (secondChildView instanceof ImageView gradientView) {
                // Album
                setHeaderGradient(viewGroup, gradientView);
            } else if (secondChildView instanceof ViewGroup thirdChildView &&
                    thirdChildView.getChildCount() == 1 &&
                    thirdChildView.getChildAt(0) instanceof ImageView gradientView) {
                // Playlist
                setHeaderGradient(viewGroup, gradientView);
            }
        });
    }

    private static void setHeaderGradient(ViewGroup viewGroup, ImageView gradientView) {
        // For some reason, it sometimes applies to other lithoViews.
        // To prevent this, check the viewId before applying the gradient.
        if (gradientView.getForeground() == null && headerGradient != null && viewGroup.getId() == elementsContainerIdentifier) {
            viewGroup.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            gradientView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            Logger.printDebug(() -> "set header gradient.\nviewGroup measured width: " + viewGroup.getMeasuredWidth() + ", gradientView measured width: " + gradientView.getMeasuredWidth());

            gradientView.setForeground(headerGradient);
        }
    }
}


