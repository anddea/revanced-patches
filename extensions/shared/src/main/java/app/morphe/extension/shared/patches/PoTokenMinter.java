/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version
 *    control systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.shared.patches;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Builds the binary response returned by the PoToken service.
 */
final class PoTokenMinter {
    private static final int MAX_INPUT_LENGTH = 768;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_ID = 0x7A64E6ED;

    private static final byte[] AES_KEY = {
            (byte) 0x20, (byte) 0xDB, (byte) 0x76, (byte) 0xF9,
            (byte) 0x03, (byte) 0xEF, (byte) 0x27, (byte) 0x45,
            (byte) 0xE9, (byte) 0x4F, (byte) 0x87, (byte) 0xBC,
            (byte) 0x2C, (byte) 0xFF, (byte) 0xE6, (byte) 0xDB,
    };

    /** Descriptor field 5 */
    private static final byte[] CHALLENGE = {
            (byte) 0x01, (byte) 0x02, (byte) 0x37, (byte) 0xFD, (byte) 0xB6,
            (byte) 0x24, (byte) 0xE5, (byte) 0x54, (byte) 0x5B, (byte) 0x85,
            (byte) 0x0B, (byte) 0xB9, (byte) 0x86, (byte) 0xFC, (byte) 0x07,
            (byte) 0x9E, (byte) 0x9C, (byte) 0x02, (byte) 0x01, (byte) 0xA3,
            (byte) 0xBC, (byte) 0x88, (byte) 0x57, (byte) 0x93, (byte) 0xAD,
            (byte) 0xB6, (byte) 0x11, (byte) 0xC0, (byte) 0xD8, (byte) 0x38,
            (byte) 0x94, (byte) 0x2A, (byte) 0x8B, (byte) 0xEA, (byte) 0xF5,
            (byte) 0xC5, (byte) 0x79, (byte) 0xA0, (byte) 0x16, (byte) 0xCF,
            (byte) 0x9D, (byte) 0xD8, (byte) 0x8B, (byte) 0x0D, (byte) 0xF1,
            (byte) 0x5D, (byte) 0x71, (byte) 0x87, (byte) 0x1C, (byte) 0x03,
            (byte) 0x97, (byte) 0xAB, (byte) 0xFD, (byte) 0x93, (byte) 0x1E,
            (byte) 0x74, (byte) 0x18, (byte) 0x0B, (byte) 0xD8, (byte) 0xE6,
            (byte) 0x0F, (byte) 0x42, (byte) 0x29, (byte) 0x5C, (byte) 0x36,
            (byte) 0xB4, (byte) 0xDB, (byte) 0xFD, (byte) 0xB8, (byte) 0x16,
            (byte) 0x24, (byte) 0xF9, (byte) 0x2D, (byte) 0x23, (byte) 0x94,
            (byte) 0xD7, (byte) 0x20, (byte) 0x7F, (byte) 0xB9, (byte) 0x72,
            (byte) 0x27, (byte) 0xB6, (byte) 0xD6, (byte) 0x74, (byte) 0xB2,
    };

    /** IntegrityToken field 2 */
    private static final byte[] TOKEN_DATA = {
            (byte) 0x01, (byte) 0x02, (byte) 0x37, (byte) 0xFD, (byte) 0xB6,
            (byte) 0x87, (byte) 0x59, (byte) 0x5A, (byte) 0x87, (byte) 0xF2,
            (byte) 0xF5, (byte) 0x65, (byte) 0x24, (byte) 0x35, (byte) 0x77,
            (byte) 0x4D, (byte) 0x25, (byte) 0x7C, (byte) 0x35, (byte) 0xC5,
            (byte) 0x26, (byte) 0x5C, (byte) 0xA5, (byte) 0xD6, (byte) 0x8E,
            (byte) 0x40, (byte) 0xAE, (byte) 0x00, (byte) 0xF6, (byte) 0x24,
            (byte) 0x6D, (byte) 0xBE, (byte) 0x61, (byte) 0xFB, (byte) 0x20,
            (byte) 0x2D, (byte) 0xA8, (byte) 0xC9, (byte) 0xEB, (byte) 0xD8,
            (byte) 0xD8, (byte) 0xCC, (byte) 0x88, (byte) 0x43, (byte) 0x32,
            (byte) 0x60, (byte) 0x14, (byte) 0xC9, (byte) 0x32, (byte) 0x79,
            (byte) 0x19, (byte) 0x39, (byte) 0x85,
    };

    private static final byte[] DESCRIPTOR_RANDOM = {
            (byte) 0x3D, (byte) 0x7A, (byte) 0x12, (byte) 0x23, (byte) 0x01,
            (byte) 0x9A, (byte) 0xA3, (byte) 0x9D, (byte) 0x9E, (byte) 0xA0,
            (byte) 0xE3, (byte) 0x43, (byte) 0x6A, (byte) 0xB7, (byte) 0xC0,
            (byte) 0x89, (byte) 0x6B, (byte) 0xFB, (byte) 0x4F, (byte) 0xB6,
            (byte) 0x79, (byte) 0xF4, (byte) 0xDE, (byte) 0x5F, (byte) 0xE7,
            (byte) 0xC2, (byte) 0x3F, (byte) 0x32, (byte) 0x6C, (byte) 0x8F,
            (byte) 0x99, (byte) 0x4A,
    };

    private static final byte[] YOUTUBE_PACKAGE = "com.google.android.youtube".getBytes(StandardCharsets.UTF_8);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static volatile boolean isWarmedUp = false;

    private PoTokenMinter() {
    }

    /**
     * Pre-initializes Cipher and SecureRandom on a background thread
     * so that the first video playback request doesn't pay the JCE provider initialization cost.
     */
    static void warmUp() {
        if (isWarmedUp) {
            return;
        }
        Utils.runOnBackgroundThread(() -> {
            try {
                long start = System.currentTimeMillis();
                byte[] dummy = new byte[16];
                SECURE_RANDOM.nextBytes(dummy);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(
                        Cipher.ENCRYPT_MODE,
                        new SecretKeySpec(AES_KEY, "AES"),
                        new GCMParameterSpec(GCM_TAG_LENGTH_BITS, dummy, 0, GCM_IV_LENGTH)
                );
                cipher.doFinal(dummy);
                isWarmedUp = true;
                Logger.printInfo(() -> "PoTokenMinter: Crypto pre-warm completed in " + (System.currentTimeMillis() - start) + "ms");
            } catch (Throwable t) {
                Logger.printException(() -> "PoTokenMinter: Crypto pre-warm failed", t);
            }
        });
    }

    /**
     * Creates the serialized {@code PoTokenResult} expected by the GMS PoToken client.
     *
     * @param input the serialized PoToken request data
     * @return a serialized result, or an empty array if generation fails
     */
    static byte[] buildPoTokenResult(byte[] input) {
        try {
            byte[] descriptor = buildDescriptor(input);
            byte[] encryptedDescriptor = encrypt(descriptor);

            ByteArrayOutputStream integrityToken = new ByteArrayOutputStream();
            writeBytes(integrityToken, 1, encryptedDescriptor);
            writeBytes(integrityToken, 2, TOKEN_DATA);

            ByteArrayOutputStream poTokenResult = new ByteArrayOutputStream();
            writeBytes(poTokenResult, 1, integrityToken.toByteArray());
            return poTokenResult.toByteArray();
        } catch (GeneralSecurityException | RuntimeException error) {
            Logger.printException(() -> "PoTokenMinter: Failed to generate built-in PoToken", error);
            return new byte[0];
        }
    }

    private static byte[] buildDescriptor(byte[] input) {
        int inputLength = input == null ? 0 : Math.min(input.length, MAX_INPUT_LENGTH);
        ByteArrayOutputStream descriptor = new ByteArrayOutputStream();
        writeBytes(descriptor, 2, input == null ? new byte[0] : input, 0, inputLength);
        writeBytes(descriptor, 3, YOUTUBE_PACKAGE);
        writeBytes(descriptor, 4, DESCRIPTOR_RANDOM);
        writeBytes(descriptor, 5, CHALLENGE);
        return descriptor.toByteArray();
    }

    private static byte[] encrypt(byte[] data) throws GeneralSecurityException {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(AES_KEY, "AES"),
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        );

        byte[] ciphertext = cipher.doFinal(data);
        byte[] result = new byte[5 + iv.length + ciphertext.length];
        result[0] = 1;
        result[1] = (byte) (KEY_ID >>> 24);
        result[2] = (byte) (KEY_ID >>> 16);
        result[3] = (byte) (KEY_ID >>> 8);
        result[4] = (byte) KEY_ID;
        System.arraycopy(iv, 0, result, 5, iv.length);
        System.arraycopy(ciphertext, 0, result, 5 + iv.length, ciphertext.length);
        return result;
    }

    private static void writeBytes(ByteArrayOutputStream output, int fieldNumber, byte[] value) {
        writeBytes(output, fieldNumber, value, 0, value.length);
    }

    @SuppressWarnings("SameParameterValue")
    private static void writeBytes(
            ByteArrayOutputStream output,
            int fieldNumber,
            byte[] value,
            int offset,
            int length
    ) {
        writeVarint(output, (fieldNumber << 3) | 2);
        writeVarint(output, length);
        output.write(value, offset, length);
    }

    private static void writeVarint(ByteArrayOutputStream output, int value) {
        while ((value & ~0x7F) != 0) {
            output.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        output.write(value);
    }
}
