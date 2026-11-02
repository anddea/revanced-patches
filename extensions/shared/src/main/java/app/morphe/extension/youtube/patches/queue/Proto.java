// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.morphe.extension.youtube.patches.queue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** Bounded protobuf operations that preserve unknown fields byte for byte during rewrites. */
final class Proto {
    static final int WIRE_LENGTH = 2;

    /** Encoded span in the message, including the tag. */
    record Field(int number, int wireType, long varint, byte[] bytes, int start, int end) { }

    /**
     * Decodes fields in wire order; a group's bytes are its fields. Rejects malformed tags,
     * unbalanced groups and truncation.
     */
    static List<Field> fields(byte[] message) {
        Reader reader = new Reader(message);
        List<Field> fields = new ArrayList<>();
        while (reader.position < message.length) {
            int start = reader.position;
            long tag = reader.tag();
            int number = (int) (tag >>> 3);
            int wire = (int) (tag & 7);
            if (wire == 4) throw new IllegalArgumentException("Unbalanced protobuf group");
            long varint = wire == 0 ? reader.varint() : 0;
            byte[] bytes = wire == 0 ? null : reader.value(number, wire);
            fields.add(new Field(number, wire, varint, bytes, start, reader.position));
        }
        return fields;
    }

    /** The last length-delimited occurrence of {@code number}. */
    static byte[] bytes(byte[] message, int number) {
        byte[] result = null;
        for (Field field : fields(message)) {
            if (field.number == number && field.wireType == WIRE_LENGTH) result = field.bytes;
        }
        return result;
    }

    static String string(byte[] message, int number) {
        byte[] value = bytes(message, number);
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }

    /** The last varint occurrence of {@code number}. */
    static long number(byte[] message, int number, long fallback) {
        for (Field field : fields(message)) {
            if (field.number == number && field.wireType == 0) fallback = field.varint;
        }
        return fallback;
    }

    /**
     * Removes every occurrence of the given fields, then appends the non-null replacements in
     * order.
     */
    static byte[] replace(byte[] message, Map<Integer, byte[]> replacements) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(message.length);
        for (Field field : fields(message)) {
            if (!replacements.containsKey(field.number)) output.write(message, field.start, field.end - field.start);
        }
        replacements.forEach((number, value) -> {
            if (value != null) writeBytes(output, number, value);
        });
        return output.toByteArray();
    }

    /**
     * Null removes a length-delimited field; returning the same array keeps its original encoding.
     */
    static byte[] rewriteLength(byte[] message, BiFunction<Integer, byte[], byte[]> rewrite) {
        ByteArrayOutputStream output = null;
        for (Field field : fields(message)) {
            byte[] value = field.wireType == WIRE_LENGTH ? rewrite.apply(field.number, field.bytes) : field.bytes;
            boolean changed = value != field.bytes;
            if (output == null && changed) {
                output = new ByteArrayOutputStream(message.length);
                output.write(message, 0, field.start);
            }
            if (output == null) continue;
            if (!changed) output.write(message, field.start, field.end - field.start);
            else if (value != null) writeBytes(output, field.number, value);
        }
        return output == null ? message : output.toByteArray();
    }

    static final class Writer {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        Writer number(int number, long value) {
            writeVarint(output, (long) number << 3);
            writeVarint(output, value);
            return this;
        }
        Writer bytes(int number, byte[] value) {
            writeBytes(output, number, value);
            return this;
        }
        Writer string(int number, String value) { return bytes(number, value.getBytes(StandardCharsets.UTF_8)); }
        Writer message(int number, Writer value) { return bytes(number, value.toByteArray()); }
        byte[] toByteArray() { return output.toByteArray(); }
    }

    private static void writeBytes(ByteArrayOutputStream output, int number, byte[] value) {
        writeVarint(output, ((long) number << 3) | WIRE_LENGTH);
        writeVarint(output, value.length);
        output.write(value, 0, value.length);
    }

    private static void writeVarint(ByteArrayOutputStream output, long value) {
        while ((value & ~0x7FL) != 0) {
            output.write((int) (value & 0x7F) | 0x80);
            value >>>= 7;
        }
        output.write((int) value);
    }

    private static final class Reader {
        private final byte[] message;
        private int position;
        Reader(byte[] message) { this.message = message; }
        long tag() {
            long tag = varint();
            long number = tag >>> 3;
            if (number < 1 || number > 0x1FFFFFFFL) throw new IllegalArgumentException("Invalid protobuf tag");
            return tag;
        }
        long varint() {
            long value = 0;
            for (int shift = 0; shift < 64; shift += 7) {
                if (position >= message.length) throw new IllegalArgumentException("Truncated protobuf varint");
                int b = message[position++];
                if (shift == 63 && (b & 0xFE) != 0) throw new IllegalArgumentException("Malformed protobuf varint");
                value |= (long) (b & 0x7F) << shift;
                if (b >= 0) return value;
            }
            throw new IllegalArgumentException("Malformed protobuf varint");
        }
        byte[] value(int number, int wire) {
            return switch (wire) {
                case 1 -> take(8);
                case 2 -> take(varint());
                case 3 -> group(number);
                case 5 -> take(4);
                default -> throw new IllegalArgumentException("Unsupported protobuf wire type " + wire);
            };
        }
        byte[] group(int number) {
            int start = position;
            while (true) {
                int end = position;
                long tag = tag();
                int field = (int) (tag >>> 3);
                int wire = (int) (tag & 7);
                if (wire == 4) {
                    if (field != number) throw new IllegalArgumentException("Unbalanced protobuf group");
                    return Arrays.copyOfRange(message, start, end);
                }
                if (wire == 0) varint(); else value(field, wire);
            }
        }
        byte[] take(long count) {
            if (count < 0 || count > message.length - position) throw new IllegalArgumentException("Truncated protobuf field");
            byte[] value = Arrays.copyOfRange(message, position, position + (int) count);
            position += value.length;
            return value;
        }
    }
}
