package app.morphe.extension.youtube.patches.utils;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.morphe.extension.youtube.patches.player.SeekbarColorPatch;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class ProgressBarDrawable extends Drawable {

    private final Paint paint = new Paint();

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (Settings.HIDE_SEEKBAR_THUMBNAIL.get()) {
            return;
        }
        paint.setColor(SeekbarColorPatch.getSeekbarColor());
        canvas.drawRect(getBounds(), paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}
