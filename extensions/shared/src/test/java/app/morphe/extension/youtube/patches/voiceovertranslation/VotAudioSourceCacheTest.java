package app.morphe.extension.youtube.patches.voiceovertranslation;

import app.morphe.extension.shared.innertube.utils.PlayerResponseOuterClass.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class VotAudioSourceCacheTest {
    private static final long NOW = 1_000_000;
    private static final String URL = "https://rr1---sn.example.googlevideo.com/videoplayback?clen=42&sig=keep-me&pot=keep-too";

    private static Format audio(int itag, int bitrate) {
        return Format.newBuilder().setItag(itag).setBitrate(bitrate).setContentLength(42)
                .setMimeType("audio/webm; codecs=\"opus\"").setUrl(URL).build();
    }
    private static void put(String id, StreamingData data) throws Exception {
        VotAudioSourceCache.put(data.toByteArray(), VideoDetails.newBuilder().setVideoId(id).build().toByteArray(), NOW);
    }
    private static StreamingData stream(Format... formats) {
        StreamingData.Builder data = StreamingData.newBuilder().setExpiresInSeconds(60);
        for (Format format : formats) data.addAdaptiveFormats(format);
        return data.build();
    }

    @Test public void preservesNativeSignedUrlAndSize() throws Exception {
        put("native", stream(audio(251, 120000), audio(249, 50000)));
        var sources = VotAudioSourceCache.get("native", NOW);
        assertEquals(249, sources.get(0).itag());
        assertEquals(URL, sources.get(0).url());
        assertEquals(42, sources.get(0).fileSize());
        assertNull(sources.get(0).userAgent());
    }
    @Test public void responseVideoIdPreventsCrossVideoReuse() throws Exception {
        put("first", stream(audio(249, 50000)));
        put("preloaded-second", stream(audio(251, 120000)));
        assertEquals(249, VotAudioSourceCache.get("first", NOW).get(0).itag());
        assertEquals(251, VotAudioSourceCache.get("preloaded-second", NOW).get(0).itag());
        assertTrue(VotAudioSourceCache.get("unknown", NOW).isEmpty());
    }
    @Test public void ignoresVideoNonHttpsAndForeignHosts() throws Exception {
        put("invalid", stream(
                audio(1, 1).toBuilder().setMimeType("video/webm").build(),
                audio(2, 2).toBuilder().setUrl("http://r.googlevideo.com/videoplayback").build(),
                audio(3, 3).toBuilder().setUrl("https://googlevideo.com.example/videoplayback").build(),
                audio(4, 4).toBuilder().setUrl("https://r.googlevideo.com/not-media").build(),
                audio(5, 5).toBuilder().setUrl("").build()));
        assertTrue(VotAudioSourceCache.get("invalid", NOW).isEmpty());
    }
    @Test public void usesDefaultAudioTrackWhenResponseHasMultipleLanguages() throws Exception {
        Format selected = audio(251, 120000).toBuilder().setAudioTrack(
                AudioTrack.newBuilder().setId("en.4").setAudioIsDefault(true)).build();
        put("languages", stream(audio(249, 50000), selected));
        assertEquals(1, VotAudioSourceCache.get("languages", NOW).size());
        assertEquals(251, VotAudioSourceCache.get("languages", NOW).get(0).itag());
    }
    @Test public void expiresSourcesAndClearsReplacedEmptyResponse() throws Exception {
        put("expiry", stream(audio(249, 50000)));
        assertFalse(VotAudioSourceCache.get("expiry", NOW + 59999).isEmpty());
        assertTrue(VotAudioSourceCache.get("expiry", NOW + 60000).isEmpty());
        put("empty", stream(audio(249, 50000)));
        put("empty", stream());
        assertTrue(VotAudioSourceCache.get("empty", NOW).isEmpty());
    }
    @Test public void boundsVideoAndFormatRetention() throws Exception {
        for (int i = 0; i < 9; i++) put("bounded-" + i, stream(audio(1, 1), audio(2, 2), audio(3, 3), audio(4, 4)));
        assertTrue(VotAudioSourceCache.get("bounded-0", NOW).isEmpty());
        assertEquals(3, VotAudioSourceCache.get("bounded-8", NOW).size());
    }
}
