package com.liskovsoft.sharedutils.helpers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Random;

import app.morphe.extension.shared.utils.Logger;

public final class Helpers {
    private static Random sRandom;

    public static String decode(String urlDecoded) {
        try {
            urlDecoded = URLDecoder.decode(urlDecoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.printException(() -> "decode failed", e);
        }
        return urlDecoded;
    }

    public static Random getRandom() {
        if (sRandom == null) {
            sRandom = new Random();
        }

        return sRandom;
    }

    public static boolean isInteger(String s) {
        return s != null && s.matches("^[-+]?\\d+$");
    }

    public static int parseInt(String numString) {
        return parseInt(numString, -1);
    }

    public static int parseInt(String numString, int defaultValue) {
        if (!isInteger(numString)) {
            return defaultValue;
        }

        return Integer.parseInt(numString);
    }
}
