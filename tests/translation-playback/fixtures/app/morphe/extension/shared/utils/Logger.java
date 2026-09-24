package app.morphe.extension.shared.utils;

import java.util.function.Supplier;

public class Logger {
    public static void printDebug(Supplier<String> s) {}

    public static void printException(Supplier<String> s, Exception e) {}
}
