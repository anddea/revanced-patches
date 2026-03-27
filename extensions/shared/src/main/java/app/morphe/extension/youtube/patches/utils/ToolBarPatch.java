package app.morphe.extension.youtube.patches.utils;

import android.view.View;
import android.widget.ImageView;

import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class ToolBarPatch {

    public static void hookToolBar(Enum<?> buttonEnum, ImageView imageView) {
        final String enumString = buttonEnum.name();
        if (enumString.isEmpty() ||
                imageView == null ||
                !(imageView.getParent() instanceof View view)) {
            return;
        }

        Logger.printDebug(() -> "enumString: " + enumString);

        hookToolBar(enumString, view);
    }

    private static void hookToolBar(String enumString, View parentView) {
    }
}


