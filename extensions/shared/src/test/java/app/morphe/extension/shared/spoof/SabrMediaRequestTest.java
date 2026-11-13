package app.morphe.extension.shared.spoof;

import com.google.protobuf.CodedOutputStream;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** Verifies request field preservation and UMP parsing across native response chunks. */
public class SabrMediaRequestTest {
    private static byte[] message(int field, byte[] value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(bytes);
        output.writeByteArray(field, value);
        output.flush();
        return bytes.toByteArray();
    }

    private static byte[] join(byte[]... parts) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (byte[] part : parts) bytes.write(part);
        return bytes.toByteArray();
    }

    @Test public void replacementRemovesAppTokenAndPreservesOtherFields() throws Exception {
        byte[] replacement = { (byte) 0x80, 1, 7 };
        byte[] originalContext = join(message(1, new byte[]{1}), message(2, new byte[]{2}),
                message(3, new byte[]{3}), message(1, new byte[]{4}));
        byte[] original = join(message(4, new byte[]{5}), message(19, originalContext), message(20, new byte[]{6}));
        byte[] expectedContext = join(message(1, replacement), message(3, new byte[]{3}));
        assertArrayEquals(join(message(4, new byte[]{5}), message(19, expectedContext), message(20, new byte[]{6})),
                SabrRequestBody.replaceClientInfo(original, replacement));
        assertArrayEquals(new byte[]{2}, SabrRequestBody.readMessageField(originalContext, 2));
        assertNull(SabrRequestBody.readMessageField(expectedContext, 2));
    }

    @Test public void missingStreamerContextLeavesRequestUnchanged() throws Exception {
        byte[] original = message(4, new byte[]{1, 2, 3});
        assertArrayEquals(original, SabrRequestBody.replaceClientInfo(original, new byte[]{9}));
    }

    @Test public void responseCanSplitAtEveryBoundaryWithoutMovingNativeBuffers() {
        byte[] response = {21, 3, 1, 2, 3, 58, 2, 8, 3};
        for (int split = 0; split <= response.length; split++) {
            SabrResponseReader reader = new SabrResponseReader();
            ByteBuffer first = ByteBuffer.wrap(response, 0, split);
            ByteBuffer second = ByteBuffer.wrap(response, split, response.length - split);
            int firstPosition = first.position(), secondPosition = second.position();
            int status = reader.read(first);
            int next = reader.read(second);
            assertEquals(3, next < 0 ? status : next);
            assertTrue(reader.sawMedia());
            assertEquals(firstPosition, first.position());
            assertEquals(secondPosition, second.position());
        }
    }

    @Test public void listenersOnlyReceiveRequestedControlParts() {
        List<Long> types = new ArrayList<>();
        List<byte[]> parts = new ArrayList<>();
        SabrResponseReader reader = new SabrResponseReader((type, part) -> {
            types.add(type);
            parts.add(part);
        }, 44);
        reader.read(ByteBuffer.wrap(new byte[]{21, 2, 9, 9, 44, 2, 1, 2, 46, 1, 3}));
        assertEquals(List.of(44L), types);
        assertArrayEquals(new byte[]{1, 2}, parts.get(0));
        assertTrue(reader.sawMedia());
    }

    @Test(expected = IllegalStateException.class)
    public void oversizedControlPartIsRejectedBeforeAllocation() {
        new SabrResponseReader((type, part) -> fail("Oversized payload delivered"), 44)
                .read(ByteBuffer.wrap(new byte[]{44, (byte) 0x81, 16}));
    }
}
