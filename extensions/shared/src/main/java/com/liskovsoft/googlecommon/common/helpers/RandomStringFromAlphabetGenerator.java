package com.liskovsoft.googlecommon.common.helpers;

import androidx.annotation.NonNull;

import com.liskovsoft.sharedutils.helpers.Helpers;

import java.util.Random;

/**
 * Generates a random string from a predefined alphabet.
 */
public final class RandomStringFromAlphabetGenerator {
    private static final String CONTENT_PLAYBACK_NONCE_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private RandomStringFromAlphabetGenerator() {
        // No impl
    }

    /**
     * Generate a random string from an alphabet.
     *
     * @param alphabet the characters' alphabet to use
     * @param length   the length of the returned string (greater than 0)
     * @param random   {@link Random} (or better {@link java.security.SecureRandom}) used for
     *                 generating the random string
     * @return a random string of the requested length made of only characters from the provided
     * alphabet
     */
    @NonNull
    public static String generate(
            final String alphabet,
            final int length,
            final Random random) {
        final StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stringBuilder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return stringBuilder.toString();
    }

    /**
     * Generate a random string from an alphabet.
     *
     * @param length   the length of the returned string (greater than 0)
     * @return a random string of the requested length made of only characters from the provided
     * alphabet
     */
    @NonNull
    public static String generate(
            final int length) {
        return generate(CONTENT_PLAYBACK_NONCE_ALPHABET, length, Helpers.getRandom());
    }
}
