package app.morphe.extension.shared;

import androidx.annotation.Nullable;

public final class Logger {
    private Logger() {
    }

    public static void printDebug(app.morphe.extension.shared.utils.Logger.LogMessage message) {
        app.morphe.extension.shared.utils.Logger.printDebug(message);
    }

    public static void printDebug(app.morphe.extension.shared.utils.Logger.LogMessage message, @Nullable Exception ex) {
        app.morphe.extension.shared.utils.Logger.printDebug(message, ex);
    }

    public static void printInfo(app.morphe.extension.shared.utils.Logger.LogMessage message) {
        app.morphe.extension.shared.utils.Logger.printInfo(message);
    }

    public static void printInfo(app.morphe.extension.shared.utils.Logger.LogMessage message, @Nullable Exception ex) {
        app.morphe.extension.shared.utils.Logger.printInfo(message, ex);
    }

    public static void printWarn(app.morphe.extension.shared.utils.Logger.LogMessage message) {
        app.morphe.extension.shared.utils.Logger.printWarn(message);
    }

    public static void printWarn(app.morphe.extension.shared.utils.Logger.LogMessage message, @Nullable Exception ex) {
        app.morphe.extension.shared.utils.Logger.printWarn(message, ex);
    }

    public static void printException(app.morphe.extension.shared.utils.Logger.LogMessage message) {
        app.morphe.extension.shared.utils.Logger.printException(message);
    }

    public static void printException(app.morphe.extension.shared.utils.Logger.LogMessage message, @Nullable Throwable ex) {
        app.morphe.extension.shared.utils.Logger.printException(message, ex);
    }

    public static void printWTF(app.morphe.extension.shared.utils.Logger.LogMessage message) {
        app.morphe.extension.shared.utils.Logger.printWTF(message);
    }
}
