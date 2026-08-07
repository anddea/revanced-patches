package app.morphe.extension.youtube.whitelist;

import java.io.Serializable;

public record VideoChannel(String channelName, String channelId) implements Serializable {}
