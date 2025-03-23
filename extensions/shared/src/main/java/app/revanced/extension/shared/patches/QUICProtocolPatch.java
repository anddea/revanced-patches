package app.revanced.extension.shared.patches;

import app.revanced.extension.shared.settings.BaseSettings;

@SuppressWarnings("unused")
public class QUICProtocolPatch {

    public static boolean disableQUICProtocol(boolean original) {
        return !BaseSettings.DISABLE_QUIC_PROTOCOL.get() && original;
    }
}
