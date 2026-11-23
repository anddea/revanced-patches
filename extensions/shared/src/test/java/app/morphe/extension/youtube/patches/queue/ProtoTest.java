package app.morphe.extension.youtube.patches.queue;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class ProtoTest {
    @Test public void rewritingMenuPreservesUnknownFieldsAndTheirEncoding() {
        // Field 1 uses a non-canonical varint encoding; field 3 is an unknown group.
        byte[] message = {8, (byte) 0x81, 0, 18, 1, 65, 27, 8, 7, 28};
        byte[] rewritten = Proto.rewriteLength(message, (number, bytes) -> new byte[]{66, 67});
        assertArrayEquals(new byte[]{8, (byte) 0x81, 0, 18, 2, 66, 67, 27, 8, 7, 28}, rewritten);
        assertSame(message, Proto.rewriteLength(message, (number, bytes) -> bytes));
    }

    @Test public void replacementRemovesAllOccurrencesAndAppendsInOrder() {
        byte[] message = {10, 1, 65, 16, 7, 10, 1, 66, 26, 1, 67};
        Map<Integer, byte[]> replacements = new LinkedHashMap<>();
        replacements.put(1, new byte[]{68});
        replacements.put(3, null);
        assertArrayEquals(new byte[]{16, 7, 10, 1, 68}, Proto.replace(message, replacements));
    }

    @Test public void fieldLookupUsesLastOccurrenceWithMatchingWireType() {
        byte[] message = {10, 1, 65, 8, 7, 10, 1, 66, 8, 9};
        assertEquals("B", Proto.string(message, 1));
        assertEquals(9, Proto.number(message, 1, 0));
    }

    @Test public void queueEndpointNumbersAndUnsignedVarintsRoundTrip() {
        byte[] command = new Proto.Writer().message(469942096,
                new Proto.Writer().message(1, new Proto.Writer().message(465666947,
                        new Proto.Writer().string(1, "queue-token")))).number(2, -1L).toByteArray();
        byte[] state = Proto.bytes(command, 469942096);
        assertEquals("queue-token", Proto.string(Proto.bytes(Proto.bytes(state, 1), 465666947), 1));
        assertEquals(-1L, Proto.number(command, 2, 0));
    }

    @Test public void malformedAndTruncatedMessagesAreRejected() {
        byte[][] invalid = {
                {0}, {8, (byte) 0x80}, {10, 2, 1}, {11, 20}, {12}, {15},
                {8, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 2}
        };
        for (byte[] message : invalid) assertThrows(IllegalArgumentException.class, () -> Proto.fields(message));
    }
}
