package app.revanced.extension.music.patches.utils;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.apache.commons.lang3.ArrayUtils;

import app.revanced.extension.shared.utils.ResourceUtils;

@SuppressWarnings("unused")
public class DrawableColorPatch {
    private static final int[] DARK_VALUES = {
            -14606047, // comments box background
            -16579837, // button container background in album
            -16777216, // button container background in playlist
    };

    // background colors
    private static final Drawable headerGradient =
            ResourceUtils.getDrawable("revanced_header_gradient");
    private static final int blackColor =
            ResourceUtils.getColor("yt_black1");
    private static final int elementsContainerIdentifier =
            ResourceUtils.getIdIdentifier("elements_container");

    public static int getLithoColor(int originalValue) {
        return ArrayUtils.contains(DARK_VALUES, originalValue)
                ? blackColor
                : originalValue;
    }

    public static void setHeaderGradient(ViewGroup viewGroup) {
        viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!(viewGroup instanceof FrameLayout frameLayout))
                return;
            if (!(frameLayout.getChildAt(0) instanceof ViewGroup firstChildView))
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
        if (headerGradient != null && viewGroup.getId() == elementsContainerIdentifier) {
            gradientView.setForeground(headerGradient);
        }
    }
}


