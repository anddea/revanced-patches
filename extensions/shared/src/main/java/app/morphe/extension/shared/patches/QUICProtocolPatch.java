package app.morphe.extension.shared.patches;

import app.morphe.extension.shared.settings.BaseSettings;

@SuppressWarnings("unused")
public class QUICProtocolPatch {

    public static boolean disableQUICProtocol(boolean original) {
        return !BaseSettings.DISABLE_QUIC_PROTOCOL.get() && original;
    }
}
