package com.airbnb.lottie;

import android.content.Context;
import android.widget.ImageView;
import java.io.InputStream;

public class LottieAnimationView extends ImageView {

    public LottieAnimationView(Context context) {
        super(context);
    }

    @SuppressWarnings("unused")
    public void setAnimation(final int rawRes) {
    }

    public void patch_setAnimation(InputStream stream, String cacheKey) {
        throw new RuntimeException("stub");
    }

    public final void patch_setAnimation(int rawResInt) {
        throw new RuntimeException("stub");
    }
}