package app.morphe.extension.shared.patches.spoof;

import androidx.annotation.Nullable;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass.StreamingData;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import app.morphe.extension.shared.utils.Logger;

public class StreamingDataOuterClassPatch extends SpoofStreamingDataPatch {
    public interface StreamingDataMessage {
        // Methods are added to YT classes during patching.
        StreamingData parseFrom(ByteBuffer responseProto);
    }


    /**
     * Do not use {@link WeakReference}.
     * This class can be null, as hooking and invoking are performed in different methods.
     */
    @Nullable
    private static StreamingDataMessage streamingDataMessage;

    /**
     * Injection point.
     */
    public static void initialize(StreamingDataMessage message) {
        if (SPOOF_STREAMING_DATA && message != null) {
            streamingDataMessage = message;
        }
    }

    /**
     * Parse the Proto Buffer and convert it to StreamingData (GeneratedMessage).
     *
     * @param responseProto Proto Buffer.
     * @return StreamingData (GeneratedMessage) parsed by ProtoParser.
     */
    @Nullable
    public static StreamingData parseFrom(ByteBuffer responseProto) {
        try {
            if (streamingDataMessage == null) {
                Logger.printDebug(() -> "Cannot parseFrom because streaming data is null");
            } else {
                return streamingDataMessage.parseFrom(responseProto);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "parseFrom failure", ex);
        }
        return null;
    }

}
