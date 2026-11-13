/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2719
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.misc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.view.KeyEvent;

import androidx.annotation.Nullable;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.ShortsPlayerState;
import app.morphe.extension.youtube.shared.VideoState;

@SuppressWarnings("unused")
public class BackgroundPlaybackPatch {

    public enum AutoPauseOnLockMode {
        OFF,
        ALWAYS,
        EXCEPT_WIRELESS_AUDIO
    }

    private static final BooleanSetting DISABLE_SHORTS_BACKGROUND_PLAYBACK =
            Settings.DISABLE_SHORTS_BACKGROUND_PLAYBACK;

    public interface BackgroundPlaybackSetting {
        /**
         * @return If the background playback setting of YouTube allows background playback now.
         */
        boolean patch_isBackgroundPlaybackAllowed();
    }

    private static boolean receiverRegistered;

    @Nullable
    private static volatile BackgroundPlaybackSetting backgroundPlaybackSetting;


    /**
     * Injection point. Called during app initialization via onCreateHook.
     */
    public static void initialize() {
        if (receiverRegistered) {
            return;
        }
        try {
            Utils.getContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent != null && Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                        handleScreenOff(context);
                    }
                }
            }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        } finally {
            receiverRegistered = true;
        }
    }

    private static void handleScreenOff(Context context) {
        AutoPauseOnLockMode mode = Settings.AUTO_PAUSE_ON_LOCK.get();
        if (mode == AutoPauseOnLockMode.OFF || VideoState.getCurrent() != VideoState.PLAYING) {
            return;
        }

        if (mode == AutoPauseOnLockMode.EXCEPT_WIRELESS_AUDIO && isWirelessAudioConnected(context)) {
            return;
        }

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            final long now = SystemClock.uptimeMillis();
            am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0));
            am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0));
        }
    }

    private static boolean isWirelessAudioConnected(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return false;

        // noinspection WrongConstant // Suppress bogus IDE warning.
        AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            final int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    || type == AudioDeviceInfo.TYPE_HEARING_AID) {
                return true;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (type == AudioDeviceInfo.TYPE_BLE_HEADSET
                        || type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                        || type == AudioDeviceInfo.TYPE_BLE_BROADCAST) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Injection point.
     */
    public static boolean isBackgroundPlaybackAllowed(boolean original) {
        if (original) return true;
        return ShortsPlayerState.getCurrent().isClosed() &&
                // 1. Shorts background playback is enabled.
                // 2. Autoplay in feed is turned on.
                // 3. Play Shorts from feed.
                // 4. Media controls appear in status bar.
                // (For unpatched YouTube with Premium accounts, media controls do not appear in the status bar)
                //
                // This is just a visual bug and does not affect Shorts background play in any way.
                // To fix this, just check PlayerType.
                PlayerType.getCurrent() != PlayerType.INLINE_MINIMAL;
    }

    /**
     * Injection point.
     */
    public static boolean isBackgroundShortsPlaybackAllowed(boolean original) {
        return !DISABLE_SHORTS_BACKGROUND_PLAYBACK.get();
    }

    /**
     * Injection point.
     */
    public static void setBackgroundPlaybackSetting(BackgroundPlaybackSetting setting) {
        backgroundPlaybackSetting = setting;
    }

    /**
     * Injection point.
     */
    public static boolean isAutomaticForegroundPlaybackAllowed(boolean original) {
        if (!original) return original;

        // When the background playback setting of YouTube does not allow background playback,
        // the app pauses the video when it goes to the background, and the original value
        // resumes the video if it was playing when the app returns to the foreground.
        BackgroundPlaybackSetting setting = backgroundPlaybackSetting;
        try {
            return setting != null && !setting.patch_isBackgroundPlaybackAllowed();
        } catch (Exception ex) {
            Logger.printException(() -> "isAutomaticForegroundPlaybackAllowed failure", ex);
            return false;
        }
    }

    /**
     * Injection point.
     */
    public static boolean isAutomaticPlaybackPauseInFlyout(boolean original) {
        return false;
    }
}
