package app.revanced.extension.youtube.patches.misc;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class QUICProtocolPatch {

    public static boolean disableQUICProtocol(boolean original) {
        try {
            return !Settings.DISABLE_QUIC_PROTOCOL.get() && original;
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to load disableQUICProtocol", ex);
        }
        return original;
    }
}
