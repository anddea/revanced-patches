package app.morphe.extension.youtube.whitelist;

import java.io.Serializable;

public final class VideoChannel implements Serializable {
    private final String channelName;
    private final String channelId;

    public VideoChannel(String channelName, String channelId) {
        this.channelName = channelName;
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getChannelId() {
        return channelId;
    }
}
