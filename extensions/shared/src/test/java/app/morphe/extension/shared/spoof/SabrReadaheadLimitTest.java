package app.morphe.extension.shared.spoof;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/** Checks native-visible policy edits, preserved framing, and safe uneditable-buffer handling. */
public class SabrReadaheadLimitTest {
    private static byte[] fields(int... pairs) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(bytes);
        for (int i = 0; i < pairs.length; i += 2) out.writeInt32(pairs[i], pairs[i + 1]);
        out.flush();
        return bytes.toByteArray();
    }

    private static byte[] part(int type, byte[] payload) {
        byte[] result = new byte[payload.length + 2];
        result[0] = (byte) type;
        result[1] = (byte) payload.length;
        System.arraycopy(payload, 0, result, 2, payload.length);
        return result;
    }

    @Test
    public void limitsBothTargetsWithoutMovingNativeBufferOrChangingOtherFields() throws Exception {
        Object callbacks = new Object();
        SabrReadaheadLimit.onRequest(callbacks);
        // A very high observed bitrate makes the 10-second minimum the limiting target.
        SabrReadaheadLimit.onResponseChunk(callbacks,
                ByteBuffer.wrap(part(20, fields(3, 999, 12, 1, 14, Integer.MAX_VALUE))));
        byte[] policy = part(35, fields(1, 1_000_000, 2, 1_000_000, 3, 123));
        ByteBuffer chunk = ByteBuffer.allocateDirect(policy.length + 3);
        chunk.position(3);
        chunk.put(policy);
        chunk.flip();
        chunk.position(3);
        int position = chunk.position(), limit = chunk.limit();
        SabrReadaheadLimit.onResponseChunk(callbacks, chunk);
        assertEquals(position, chunk.position());
        assertEquals(limit, chunk.limit());
        byte[] edited = new byte[policy.length];
        chunk.duplicate().get(edited);
        assertEquals(35, edited[0]);
        assertEquals(policy[1], edited[1]);
        CodedInputStream input = CodedInputStream.newInstance(edited, 2, edited.length - 2);
        assertEquals(8, input.readTag()); assertEquals(10_000, input.readInt32());
        assertEquals(16, input.readTag()); assertEquals(10_000, input.readInt32());
        assertEquals(24, input.readTag()); assertEquals(123, input.readInt32());
        assertEquals(0, input.readTag());
    }

    @Test
    public void splitPayloadAndReadOnlyBufferAreNeverEdited() {
        int[] calls = {0};
        SabrResponseReader reader = new SabrResponseReader((buffer, offset, length) -> calls[0]++, 35, null);
        reader.read(ByteBuffer.wrap(new byte[]{35, 3, 8}));
        reader.read(ByteBuffer.wrap(new byte[]{1, 0}));
        reader.read(ByteBuffer.wrap(new byte[]{35, 2, 8, 1}).asReadOnlyBuffer());
        assertEquals(0, calls[0]);
    }

    @Test
    public void editsWholePayloadAfterSplitHeaderAndKeepsMediaBytes() {
        SabrResponseReader reader = new SabrResponseReader((buffer, offset, length) -> buffer.put(offset, (byte) 9), 35, null);
        reader.read(ByteBuffer.wrap(new byte[]{35}));
        byte[] next = {2, 8, 1, 21, 2, 7, 6};
        reader.read(ByteBuffer.wrap(next));
        assertArrayEquals(new byte[]{2, 9, 1, 21, 2, 7, 6}, next);
        assertTrue(reader.sawMedia());
    }
}
