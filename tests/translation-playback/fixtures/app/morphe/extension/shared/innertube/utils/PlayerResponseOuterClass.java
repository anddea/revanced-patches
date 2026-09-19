package app.morphe.extension.shared.innertube.utils;

import com.google.protobuf.MessageLite;

public class PlayerResponseOuterClass {
    public static class VideoDetails implements MessageLite {
        private final String id;

        public VideoDetails(String id) {
            this.id = id;
        }

        public byte[] toByteArray() {
            return id.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        public static VideoDetails parseFrom(byte[] bytes) {
            return new VideoDetails(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        }

        public String getVideoId() {
            return id;
        }

        public String getChannelId() {
            return "channel";
        }

        public String getAuthor() {
            return "author";
        }

        public String getTitle() {
            return "title";
        }

        public long getLengthSeconds() {
            return 100;
        }

        public boolean getIsLiveContent() {
            return true;
        }
    }
}
