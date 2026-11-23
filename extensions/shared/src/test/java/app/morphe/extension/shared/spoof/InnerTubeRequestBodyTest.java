package app.morphe.extension.shared.spoof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.google.protobuf.CodedOutputStream;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/** Checks direct, nested, compressed, and malformed request bodies without making network requests. */
public class InnerTubeRequestBodyTest {
    private static byte[] field(byte[] value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(bytes);
        output.writeByteArray(2, value);
        output.flush();
        return bytes.toByteArray();
    }

    private static byte[] gzip(byte[] body) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream output = new GZIPOutputStream(bytes)) {
            output.write(body);
        }
        return bytes.toByteArray();
    }

    @Test
    public void readsDirectAndNestedVideoIds() throws Exception {
        byte[] player = field("abcdefghijk".getBytes(StandardCharsets.UTF_8));
        assertEquals("abcdefghijk", InnerTubeRequestBody.readVideoId(player, false));
        assertEquals("abcdefghijk", InnerTubeRequestBody.readVideoId(field(player), true));
    }

    @Test
    public void readsCompressedBodies() throws Exception {
        byte[] player = field("abcdefghijk".getBytes(StandardCharsets.UTF_8));
        assertEquals("abcdefghijk", InnerTubeRequestBody.readVideoId(gzip(player), false));
        assertEquals("abcdefghijk", InnerTubeRequestBody.readVideoId(gzip(field(player)), true));
    }

    @Test
    public void absentVideoIdsDoNotStartAFetch() throws Exception {
        assertNull(InnerTubeRequestBody.readVideoId(new byte[0], false));
        assertNull(InnerTubeRequestBody.readVideoId(new byte[0], true));
        assertNull(InnerTubeRequestBody.readVideoId(field(new byte[0]), true));
    }

    @Test
    public void malformedBodiesFailWithoutReturningAnId() {
        assertThrows(IOException.class,
                () -> InnerTubeRequestBody.readVideoId(new byte[]{0x12, 0x7F}, false));
        assertThrows(IOException.class,
                () -> InnerTubeRequestBody.readVideoId(new byte[]{0x1F, (byte) 0x8B, 0}, false));
    }
}
