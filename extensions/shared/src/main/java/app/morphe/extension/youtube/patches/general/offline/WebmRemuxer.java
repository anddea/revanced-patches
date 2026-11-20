/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 * Written by lavinhoque33, 2026-10-15.
 */

package app.morphe.extension.youtube.patches.general.offline;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Joins one WebM video file and one WebM audio file (YouTube DASH: VP9 and Opus) into one WebM
 * without touching a frame. {@link android.media.MediaMuxer} needs a native call per sample and took
 * about 75 seconds for a 1.9 GB file on a phone; this reads the block headers, builds a new
 * {@code Info}, {@code Tracks}, {@code Cues} and {@code SeekHead}, and copies the frame bytes in large
 * blocks.
 * <p>
 * The output is written sequentially: EBML header, a {@code Segment} of exactly known size,
 * {@code SeekHead}, {@code Info}, {@code Tracks}, {@code Cues} (one per video keyframe, so seeking
 * works), then clusters holding the blocks of both tracks interleaved by timecode. Every video
 * keyframe starts a cluster. The {@code TrackEntry} of each input is carried over element by element
 * (so the Opus {@code CodecPrivate}, {@code CodecDelay} and {@code SeekPreRoll} survive); only the
 * track number and uid are replaced. {@code BlockGroup} extras ({@code BlockDuration},
 * {@code DiscardPadding}, {@code BlockAdditions}, ...) are copied as they are.
 * <p>
 * Anything this class does not rebuild raises {@link Mp4Remuxer.UnsupportedInputException} and the
 * caller falls back to the platform muxer: more than one track in an input, a track of the wrong
 * kind or an unknown codec, {@code ContentEncodings} (compression or encryption), lacing, a timecode
 * scale other than 1 ms, elements of unknown size, a block of another track, a block before its
 * cluster timecode, {@code EncryptedBlock}, negative timecodes, or a file that is cut short.
 */
final class WebmRemuxer {
    private static final int ID_EBML = 0x1A45DFA3;
    private static final int ID_SEGMENT = 0x18538067;
    private static final int ID_SEEK_HEAD = 0x114D9B74;
    private static final int ID_SEEK = 0x4DBB;
    private static final int ID_SEEK_ID = 0x53AB;
    private static final int ID_SEEK_POSITION = 0x53AC;
    private static final int ID_INFO = 0x1549A966;
    private static final int ID_TIMECODE_SCALE = 0x2AD7B1;
    private static final int ID_DURATION = 0x4489;
    private static final int ID_MUXING_APP = 0x4D80;
    private static final int ID_WRITING_APP = 0x5741;
    private static final int ID_TRACKS = 0x1654AE6B;
    private static final int ID_TRACK_ENTRY = 0xAE;
    private static final int ID_TRACK_NUMBER = 0xD7;
    private static final int ID_TRACK_UID = 0x73C5;
    private static final int ID_TRACK_TYPE = 0x83;
    private static final int ID_CODEC_ID = 0x86;
    private static final int ID_CONTENT_ENCODINGS = 0x6D80;
    private static final int ID_CUES = 0x1C53BB6B;
    private static final int ID_CUE_POINT = 0xBB;
    private static final int ID_CUE_TIME = 0xB3;
    private static final int ID_CUE_TRACK_POSITIONS = 0xB7;
    private static final int ID_CUE_TRACK = 0xF7;
    private static final int ID_CUE_CLUSTER_POSITION = 0xF1;
    private static final int ID_CLUSTER = 0x1F43B675;
    private static final int ID_CLUSTER_TIMECODE = 0xE7;
    private static final int ID_SIMPLE_BLOCK = 0xA3;
    private static final int ID_BLOCK_GROUP = 0xA0;
    private static final int ID_BLOCK = 0xA1;
    private static final int ID_REFERENCE_BLOCK = 0xFB;
    private static final int ID_ENCRYPTED_BLOCK = 0xAF;

    private static final int TRACK_VIDEO = 1;
    private static final int TRACK_AUDIO = 2;

    private static final int FLAG_LACING = 0x06;
    private static final int FLAG_KEYFRAME = 0x80;
    /** Bits above the flag byte of {@code Entry.meta}. */
    private static final int META_KEY = 0x100;
    private static final int META_GROUP = 0x200;

    /** Largest element read into memory whole (track entries, block group extras). */
    private static final int MAX_SMALL_ELEMENT = 1 << 20;
    private static final int WINDOW_SIZE = 4 << 20;
    private static final int SINK_SIZE = 8 << 20;
    /** A cluster is closed after this long without a keyframe, so block timecodes fit 16 bits. */
    private static final long MAX_CLUSTER_SPAN = 30000;
    private static final long UNKNOWN_SIZE = -1;
    private static final int CANCEL_CHECK_BLOCKS = 4096;

    /** Seek head entries: Info, Tracks, Cues. */
    private static final int SEEK_HEAD_ENTRIES = 3;
    /** {@code CuePoint}: id, size, CueTime(2+4), CueTrackPositions(id, size, CueTrack(3), CueClusterPosition(2+5)). */
    private static final int CUE_POINT_BYTES = 20;
    private static final int CUE_TIME_BYTES = 4;
    private static final int CUE_POSITION_BYTES = 5;

    private WebmRemuxer() {
    }

    /**
     * Writes the joined file sequentially to {@code out}, from its current position.
     *
     * @param cancelled Polled between clusters, so a cancelled download stops promptly.
     * @throws Mp4Remuxer.UnsupportedInputException If the inputs cannot be remuxed. {@code out} may have been written to.
     */
    static void remux(File video, File audio, FileChannel out, BooleanSupplier cancelled) throws IOException {
        try (RandomAccessFile videoFile = new RandomAccessFile(video, "r");
             RandomAccessFile audioFile = new RandomAccessFile(audio, "r")) {
            Input videoInput = new Input(videoFile.getChannel(), TRACK_VIDEO);
            Input audioInput = new Input(audioFile.getChannel(), TRACK_AUDIO);
            videoInput.parse(cancelled);
            audioInput.parse(cancelled);
            write(videoInput, audioInput, out, cancelled);
        }
    }

    // region output

    private static void write(Input video, Input audio, FileChannel out, BooleanSupplier cancelled)
            throws IOException {
        // Merge by timecode; at a tie the video block goes first so a keyframe opens its cluster.
        final int videoCount = video.count;
        final int audioCount = audio.count;
        final boolean[] takeVideo = new boolean[videoCount + audioCount];
        for (int v = 0, a = 0, n = 0; n < takeVideo.length; n++) {
            takeVideo[n] = v < videoCount && (a >= audioCount || video.timecode.get(v) <= audio.timecode.get(a));
            if (takeVideo[n]) v++;
            else a++;
        }

        // Cluster layout: [first block (in merge order), timecode, content size, opens with a video keyframe].
        final IntList clusterFirst = new IntList();
        final LongList clusterTimecode = new LongList();
        final LongList clusterContent = new LongList();
        final IntList clusterKeyframe = new IntList();
        long clusterBytes = 0;
        long timecode = 0;
        long content = 0;
        boolean open = false;
        boolean openKeyframe = false;
        int first = 0;
        for (int n = 0, v = 0, a = 0; n < takeVideo.length; n++) {
            final boolean isVideo = takeVideo[n];
            final Input input = isVideo ? video : audio;
            final int index = isVideo ? v++ : a++;
            final long blockTimecode = input.timecode.get(index);
            final boolean keyframe = isVideo && (input.meta.get(index) & META_KEY) != 0;
            if (open && (keyframe || blockTimecode - timecode > MAX_CLUSTER_SPAN)) {
                clusterFirst.add(first);
                clusterTimecode.add(timecode);
                clusterContent.add(content);
                clusterKeyframe.add(openKeyframe ? 1 : 0);
                clusterBytes += elementSize(ID_CLUSTER, content);
                open = false;
            }
            if (!open) {
                if (blockTimecode < 0) throw new Mp4Remuxer.UnsupportedInputException("Negative timecode");
                first = n;
                timecode = blockTimecode;
                content = 2 + uintLength(timecode); // the cluster Timecode element
                open = true;
                openKeyframe = keyframe;
            } else if (blockTimecode - timecode < Short.MIN_VALUE) {
                throw new Mp4Remuxer.UnsupportedInputException("Block timecode out of order");
            }
            content += blockSize(input, index);
        }
        if (open) {
            clusterFirst.add(first);
            clusterTimecode.add(timecode);
            clusterContent.add(content);
            clusterKeyframe.add(openKeyframe ? 1 : 0);
            clusterBytes += elementSize(ID_CLUSTER, content);
        }
        final int clusterCount = clusterFirst.size;

        int keyframes = 0;
        for (int c = 0; c < clusterCount; c++) {
            if (clusterKeyframe.get(c) != 0) keyframes++;
        }

        if (keyframes == 0) throw new Mp4Remuxer.UnsupportedInputException("No video keyframe");

        // Everything before the first cluster has a size that does not depend on any position.
        final byte[] ebml = buildEbmlHeader();
        final byte[] info = buildInfo(Math.max(Math.max(video.durationMs(), audio.durationMs()),
                Math.max(video.lastTimecode(), audio.lastTimecode())));
        final byte[] tracks = buildTracks(video, audio);
        final long cuesSize = elementSize(ID_CUES, (long) keyframes * CUE_POINT_BYTES);
        final long seekHeadSize = elementSize(ID_SEEK_HEAD, (long) SEEK_HEAD_ENTRIES * seekEntrySize());
        final long infoPosition = seekHeadSize;
        final long tracksPosition = infoPosition + info.length;
        final long cuesPosition = tracksPosition + tracks.length;
        final long firstClusterPosition = cuesPosition + cuesSize;
        final long segmentSize = firstClusterPosition + clusterBytes;

        final long[] clusterPosition = new long[clusterCount];
        long position = firstClusterPosition;
        for (int c = 0; c < clusterCount; c++) {
            clusterPosition[c] = position;
            position += elementSize(ID_CLUSTER, clusterContent.get(c));
        }

        final Buf header = new Buf();
        header.raw(ebml);
        header.id(ID_SEGMENT);
        header.sizeFixed8(segmentSize);
        header.raw(buildSeekHead(infoPosition, tracksPosition, cuesPosition));
        header.raw(info);
        header.raw(tracks);
        buildCues(header, clusterCount, clusterTimecode, clusterKeyframe, clusterPosition, keyframes);
        final long expected = header.size() + clusterBytes;
        if (header.size() - ebml.length - 12 != firstClusterPosition) {
            throw new IOException("Header layout mismatch");
        }

        final Sink sink = new Sink(out);
        sink.put(header.data, header.size());
        final Buf small = new Buf();
        final Reader videoReader = new Reader(video.channel);
        final Reader audioReader = new Reader(audio.channel);
        int v = 0;
        int a = 0;
        int n = 0;
        for (int c = 0; c < clusterCount; c++) {
            if (cancelled.getAsBoolean()) throw new IOException("Cancelled");
            final int end = c + 1 < clusterCount ? clusterFirst.get(c + 1) : takeVideo.length;
            final long clusterTc = clusterTimecode.get(c);
            small.reset();
            small.id(ID_CLUSTER);
            small.size(clusterContent.get(c));
            small.id(ID_CLUSTER_TIMECODE);
            small.size(uintLength(clusterTc));
            small.uintBytes(clusterTc);
            sink.put(small.data, small.size());
            for (; n < end; n++) {
                final boolean isVideo = takeVideo[n];
                final Input input = isVideo ? video : audio;
                final Reader reader = isVideo ? videoReader : audioReader;
                final int index = isVideo ? v++ : a++;
                writeBlock(input, index, input.timecode.get(index) - clusterTc, reader, sink, small);
            }
        }
        sink.flush();
        if (sink.total != expected) throw new IOException("Wrote " + sink.total + " of " + expected + " bytes");
    }

    private static int seekEntrySize() {
        // Seek: id (2) + size (1) + SeekID (2 + 1 + 4) + SeekPosition (2 + 1 + 5).
        return 2 + 1 + (2 + 1 + 4) + (2 + 1 + CUE_POSITION_BYTES);
    }

    private static long blockSize(Input input, int index) {
        final long payload = input.length.get(index);
        final long block = 4 + payload; // track number, timecode, flags
        if ((input.meta.get(index) & META_GROUP) == 0) return elementSize(ID_SIMPLE_BLOCK, block);
        final byte[] extra = input.extras.get(index);
        final long group = elementSize(ID_BLOCK, block) + (extra == null ? 0 : extra.length);
        return elementSize(ID_BLOCK_GROUP, group);
    }

    private static void writeBlock(Input input, int index, long relative, Reader reader, Sink sink, Buf small)
            throws IOException {
        final int meta = input.meta.get(index);
        final long payload = input.length.get(index);
        final long block = 4 + payload;
        small.reset();
        if ((meta & META_GROUP) == 0) {
            small.id(ID_SIMPLE_BLOCK);
            small.size(block);
        } else {
            final byte[] extra = input.extras.get(index);
            small.id(ID_BLOCK_GROUP);
            small.size(elementSize(ID_BLOCK, block) + (extra == null ? 0 : extra.length));
            small.id(ID_BLOCK);
            small.size(block);
        }
        small.u8(0x80 | input.outputTrack);
        small.u8((int) (relative >> 8));
        small.u8((int) relative);
        // A block's own keyframe flag (0x80) is only valid in a SimpleBlock.
        small.u8((meta & META_GROUP) == 0 ? meta & 0xFF : meta & 0x7F);
        sink.put(small.data, small.size());
        reader.copy(input.offset.get(index), payload, sink);
        if ((meta & META_GROUP) != 0) {
            final byte[] extra = input.extras.get(index);
            if (extra != null) sink.put(extra, extra.length);
        }
    }

    private static byte[] buildEbmlHeader() {
        final Buf content = new Buf();
        content.uint(0x4286, 1); // EBMLVersion
        content.uint(0x42F7, 1); // EBMLReadVersion
        content.uint(0x42F2, 4); // EBMLMaxIDLength
        content.uint(0x42F3, 8); // EBMLMaxSizeLength
        content.string(0x4282, "webm"); // DocType
        content.uint(0x4287, 4); // DocTypeVersion
        content.uint(0x4285, 2); // DocTypeReadVersion
        return element(ID_EBML, content);
    }

    private static byte[] buildSeekHead(long infoPosition, long tracksPosition, long cuesPosition) {
        final Buf content = new Buf();
        seekEntry(content, ID_INFO, infoPosition);
        seekEntry(content, ID_TRACKS, tracksPosition);
        seekEntry(content, ID_CUES, cuesPosition);
        return element(ID_SEEK_HEAD, content);
    }

    private static void seekEntry(Buf out, int id, long position) {
        final Buf entry = new Buf();
        entry.id(ID_SEEK_ID);
        entry.size(4);
        entry.raw(new byte[]{(byte) (id >>> 24), (byte) (id >>> 16), (byte) (id >>> 8), (byte) id});
        entry.id(ID_SEEK_POSITION);
        entry.size(CUE_POSITION_BYTES);
        entry.uintFixed(position, CUE_POSITION_BYTES);
        out.raw(element(ID_SEEK, entry));
    }

    private static byte[] buildInfo(double durationMs) {
        final Buf content = new Buf();
        content.uint(ID_TIMECODE_SCALE, 1_000_000);
        content.id(ID_DURATION);
        content.size(8);
        content.u64(Double.doubleToRawLongBits(durationMs));
        content.string(ID_MUXING_APP, "RVX");
        content.string(ID_WRITING_APP, "RVX");
        return element(ID_INFO, content);
    }

    private static byte[] buildTracks(Input video, Input audio) {
        final Buf content = new Buf();
        content.raw(video.trackEntry);
        content.raw(audio.trackEntry);
        return element(ID_TRACKS, content);
    }

    private static void buildCues(Buf out, int clusterCount, LongList clusterTimecode, IntList clusterKeyframe,
                                  long[] clusterPosition, int keyframes) throws IOException {
        out.id(ID_CUES);
        out.size((long) keyframes * CUE_POINT_BYTES);
        for (int c = 0; c < clusterCount; c++) {
            if (clusterKeyframe.get(c) == 0) continue;
            final long time = clusterTimecode.get(c);
            if (time >= 1L << (8 * CUE_TIME_BYTES)) throw new Mp4Remuxer.UnsupportedInputException("Cue time too large");
            if (clusterPosition[c] >= 1L << (8 * CUE_POSITION_BYTES)) {
                throw new Mp4Remuxer.UnsupportedInputException("Cue position too large");
            }
            out.id(ID_CUE_POINT);
            out.size(18);
            out.id(ID_CUE_TIME);
            out.size(CUE_TIME_BYTES);
            out.uintFixed(time, CUE_TIME_BYTES);
            out.id(ID_CUE_TRACK_POSITIONS);
            out.size(10);
            out.id(ID_CUE_TRACK);
            out.size(1);
            out.u8(TRACK_VIDEO);
            out.id(ID_CUE_CLUSTER_POSITION);
            out.size(CUE_POSITION_BYTES);
            out.uintFixed(clusterPosition[c], CUE_POSITION_BYTES);
        }
    }

    /** The whole element for {@code content} of the given id. */
    private static byte[] element(int id, Buf content) {
        final Buf out = new Buf();
        out.id(id);
        out.size(content.size());
        out.raw(content.data, content.size());
        return out.toArray();
    }

    /** Bytes of an element: id, size field, content. */
    private static long elementSize(int id, long contentSize) {
        return idLength(id) + sizeLength(contentSize) + contentSize;
    }

    private static int idLength(int id) {
        return id > 0xFFFFFF ? 4 : id > 0xFFFF ? 3 : id > 0xFF ? 2 : 1;
    }

    /** Length of the shortest size field for {@code value}; all ones is reserved for "unknown". */
    private static int sizeLength(long value) {
        int length = 1;
        while (length < 8 && value >= (1L << (7 * length)) - 1) length++;
        return length;
    }

    private static int uintLength(long value) {
        int length = 1;
        while (length < 8 && (value >>> (8 * length)) != 0) length++;
        return length;
    }

    // endregion

    // region input

    /** One WebM file with one track: the block index and the rewritten track entry. */
    private static final class Input {
        final FileChannel channel;
        final long fileLength;
        final int outputTrack;
        final ByteBuffer scratch = ByteBuffer.allocate(32);

        long timecodeScale = 1_000_000;
        double duration = -1;
        int inputTrack = -1;
        byte[] trackEntry;
        boolean haveTracks;

        int count;
        final LongList timecode = new LongList();
        final LongList offset = new LongList();
        final IntList length = new IntList();
        final IntList meta = new IntList();
        final Map<Integer, byte[]> extras = new HashMap<>();

        Input(FileChannel channel, int outputTrack) throws IOException {
            this.channel = channel;
            this.fileLength = channel.size();
            this.outputTrack = outputTrack;
        }

        double durationMs() {
            return duration < 0 ? 0 : duration * timecodeScale / 1_000_000.0;
        }

        double lastTimecode() {
            double last = 0;
            for (int i = 0; i < count; i++) last = Math.max(last, timecode.get(i));
            return last;
        }

        void parse(BooleanSupplier cancelled) throws IOException {
            // EBML header, then the segment.
            long position = 0;
            final Header header = new Header();
            if (!readHeader(position, fileLength, header) || header.id != ID_EBML) {
                throw unsupported("Not a WebM file");
            }
            position = header.end();
            if (!readHeader(position, fileLength, header) || header.id != ID_SEGMENT) {
                throw unsupported("No segment");
            }
            final long segmentEnd = header.end();
            position = header.dataStart;
            while (readHeader(position, segmentEnd, header)) {
                if (cancelled.getAsBoolean()) throw new IOException("Cancelled");
                final long next = header.end();
                switch (header.id) {
                    case ID_INFO -> parseInfo(header.dataStart, next);
                    case ID_TRACKS -> parseTracks(header.dataStart, next);
                    case ID_CLUSTER -> {
                        if (!haveTracks) throw unsupported("Cluster before the tracks");
                        parseCluster(header.dataStart, next, cancelled);
                    }
                    default -> {
                    } // SeekHead, Cues, Void, Tags, ... are rebuilt or dropped.
                }
                position = next;
            }
            if (!haveTracks) throw unsupported("No tracks");
            if (count == 0) throw unsupported("No blocks");
            if (timecodeScale != 1_000_000) throw unsupported("Timecode scale " + timecodeScale);
        }

        private void parseInfo(long start, long end) throws IOException {
            final Header child = new Header();
            for (long position = start; readHeader(position, end, child); position = child.end()) {
                if (child.id == ID_TIMECODE_SCALE) {
                    timecodeScale = readUint(child.dataStart, child.size);
                } else if (child.id == ID_DURATION) {
                    if (child.size == 4) {
                        duration = Float.intBitsToFloat((int) readUint(child.dataStart, 4));
                    } else if (child.size == 8) {
                        duration = Double.longBitsToDouble(readUint(child.dataStart, 8));
                    } else {
                        throw unsupported("Duration of " + child.size + " bytes");
                    }
                }
            }
        }

        private void parseTracks(long start, long end) throws IOException {
            if (haveTracks) throw unsupported("Two Tracks elements");
            final Header entry = new Header();
            boolean seen = false;
            for (long position = start; readHeader(position, end, entry); position = entry.end()) {
                if (entry.id != ID_TRACK_ENTRY) continue;
                if (seen) throw unsupported("More than one track in a file");
                seen = true;
                parseTrackEntry(entry.dataStart, entry.end());
            }
            if (!seen) throw unsupported("No track entry");
            haveTracks = true;
        }

        private void parseTrackEntry(long start, long end) throws IOException {
            final Buf kept = new Buf();
            final Header child = new Header();
            int type = -1;
            String codec = "";
            for (long position = start; readHeader(position, end, child); position = child.end()) {
                switch (child.id) {
                    case ID_TRACK_NUMBER -> inputTrack = (int) readUint(child.dataStart, child.size);
                    case ID_TRACK_UID -> {
                    }
                    case ID_TRACK_TYPE -> {
                        type = (int) readUint(child.dataStart, child.size);
                        keep(kept, child);
                    }
                    case ID_CODEC_ID -> {
                        codec = new String(readSmall(child.dataStart, child.size), StandardCharsets.US_ASCII);
                        keep(kept, child);
                    }
                    case ID_CONTENT_ENCODINGS -> throw unsupported("Content encoding (compression or encryption)");
                    default -> keep(kept, child);
                }
            }
            if (inputTrack <= 0) throw unsupported("Track without a number");
            if (outputTrack == TRACK_VIDEO) {
                if (type != TRACK_VIDEO || !(codec.equals("V_VP9") || codec.equals("V_VP8") || codec.equals("V_AV1"))) {
                    throw unsupported("Video track " + type + " " + codec);
                }
            } else if (type != TRACK_AUDIO || !(codec.equals("A_OPUS") || codec.equals("A_VORBIS"))) {
                throw unsupported("Audio track " + type + " " + codec);
            }
            final Buf content = new Buf();
            content.uint(ID_TRACK_NUMBER, outputTrack);
            content.uint(ID_TRACK_UID, outputTrack);
            content.raw(kept.data, kept.size());
            trackEntry = element(ID_TRACK_ENTRY, content);
        }

        /** Copies an element, header included, as it is. */
        private void keep(Buf out, Header element) throws IOException {
            final long total = element.end() - element.start;
            if (total > MAX_SMALL_ELEMENT) throw unsupported("Large track element");
            out.raw(readSmall(element.start, total));
        }

        private void parseCluster(long start, long end, BooleanSupplier cancelled) throws IOException {
            if (cancelled.getAsBoolean()) throw new IOException("Cancelled");
            long clusterTimecode = Long.MIN_VALUE;
            final Header child = new Header();
            final Header inner = new Header();
            for (long position = start; readHeader(position, end, child); position = child.end()) {
                switch (child.id) {
                    case ID_CLUSTER_TIMECODE -> clusterTimecode = readUint(child.dataStart, child.size);
                    case ID_SIMPLE_BLOCK -> {
                        if (clusterTimecode == Long.MIN_VALUE) throw unsupported("Block before the cluster timecode");
                        addBlock(child.dataStart, child.size, clusterTimecode, false, null);
                    }
                    case ID_BLOCK_GROUP -> {
                        if (clusterTimecode == Long.MIN_VALUE) throw unsupported("Block before the cluster timecode");
                        parseGroup(child.dataStart, child.end(), clusterTimecode, inner);
                    }
                    case ID_ENCRYPTED_BLOCK -> throw unsupported("Encrypted block");
                    default -> {
                    }
                }
                if ((count & (CANCEL_CHECK_BLOCKS - 1)) == 0 && cancelled.getAsBoolean()) {
                    throw new IOException("Cancelled");
                }
            }
        }

        private void parseGroup(long start, long end, long clusterTimecode, Header child) throws IOException {
            final Buf extra = new Buf();
            long blockStart = -1;
            long blockSize = 0;
            boolean reference = false;
            for (long position = start; readHeader(position, end, child); position = child.end()) {
                if (child.id == ID_BLOCK) {
                    if (blockStart >= 0) throw unsupported("Two blocks in a group");
                    blockStart = child.dataStart;
                    blockSize = child.size;
                } else {
                    if (child.id == ID_REFERENCE_BLOCK) reference = true;
                    final long total = child.end() - child.start;
                    if (total > MAX_SMALL_ELEMENT) throw unsupported("Large block group element");
                    extra.raw(readSmall(child.start, total));
                }
            }
            if (blockStart < 0) throw unsupported("Group without a block");
            addBlock(blockStart, blockSize, clusterTimecode, true, reference ? null : Boolean.TRUE);
            if (extra.size() > 0) extras.put(count - 1, extra.toArray());
        }

        /**
         * @param key Null for a SimpleBlock or a group block with a reference, TRUE for a group block
         *            without one, which is a keyframe.
         */
        private void addBlock(long start, long size, long clusterTimecode, boolean group, Boolean key)
                throws IOException {
            scratch.clear();
            final int wanted = (int) Math.min(size, 12);
            scratch.limit(wanted);
            readFully(start, scratch);
            scratch.flip();
            if (scratch.remaining() < 4) throw unsupported("Short block");
            final int first = scratch.get(0) & 0xFF;
            if (first == 0) throw unsupported("Bad track number");
            final int trackLength = Integer.numberOfLeadingZeros(first) - 23;
            if (trackLength > 1 || scratch.remaining() < trackLength + 3) {
                // Track numbers above 126 never occur in single-track files.
                throw unsupported("Block of another track");
            }
            final int trackNumber = first & 0x7F;
            if (trackNumber != inputTrack) throw unsupported("Block of another track");
            final int relative = (short) (((scratch.get(1) & 0xFF) << 8) | (scratch.get(2) & 0xFF));
            final int flags = scratch.get(3) & 0xFF;
            if ((flags & FLAG_LACING) != 0) throw unsupported("Laced block");
            final long payload = size - 4;
            if (payload < 0 || payload > Integer.MAX_VALUE) throw unsupported("Bad block size");

            int meta = flags;
            if (group) meta |= META_GROUP;
            if (group ? key != null : (flags & FLAG_KEYFRAME) != 0) meta |= META_KEY;
            this.timecode.add(clusterTimecode + relative);
            this.offset.add(start + 4);
            this.length.add((int) payload);
            this.meta.add(meta);
            count++;
        }

        // reading

        boolean readHeader(long position, long limit, Header out) throws IOException {
            if (position >= limit) return false;
            scratch.clear();
            scratch.limit((int) Math.min(scratch.capacity(), limit - position));
            readFully(position, scratch);
            scratch.flip();
            if (scratch.remaining() < 2) throw unsupported("Truncated element");
            final int idFirst = scratch.get(0) & 0xFF;
            if (idFirst == 0) throw unsupported("Bad element id");
            final int idLength = Integer.numberOfLeadingZeros(idFirst) - 23;
            if (idLength > 4 || scratch.remaining() < idLength + 1) throw unsupported("Bad element id");
            int id = 0;
            for (int i = 0; i < idLength; i++) id = (id << 8) | (scratch.get(i) & 0xFF);
            final int sizeFirst = scratch.get(idLength) & 0xFF;
            if (sizeFirst == 0) throw unsupported("Bad element size");
            final int sizeLength = Integer.numberOfLeadingZeros(sizeFirst) - 23;
            if (scratch.remaining() < idLength + sizeLength) throw unsupported("Truncated element");
            long size = sizeFirst & (0xFF >> sizeLength);
            boolean allOnes = size == (0xFF >> sizeLength);
            for (int i = 1; i < sizeLength; i++) {
                final int b = scratch.get(idLength + i) & 0xFF;
                size = (size << 8) | b;
                allOnes &= b == 0xFF;
            }
            if (allOnes) throw unsupported("Element of unknown size");
            out.id = id;
            out.start = position;
            out.dataStart = position + idLength + sizeLength;
            out.size = size;
            if (out.dataStart + size > limit) throw unsupported("Element past its parent");
            return true;
        }

        long readUint(long position, long size) throws IOException {
            if (size < 1 || size > 8) throw unsupported("Bad integer size");
            final byte[] data = readSmall(position, size);
            long value = 0;
            for (byte b : data) value = (value << 8) | (b & 0xFF);
            return value;
        }

        byte[] readSmall(long position, long size) throws IOException {
            if (size > MAX_SMALL_ELEMENT) throw unsupported("Large element");
            final ByteBuffer buffer = ByteBuffer.allocate((int) size);
            readFully(position, buffer);
            return buffer.array();
        }

        private void readFully(long position, ByteBuffer buffer) throws IOException {
            while (buffer.hasRemaining()) {
                // The buffers read here are filled from their start.
                final int read = channel.read(buffer, position + buffer.position());
                if (read < 0) throw unsupported("Truncated file");
            }
        }
    }

    private static Mp4Remuxer.UnsupportedInputException unsupported(String message) {
        return new Mp4Remuxer.UnsupportedInputException(message);
    }

    /** An element header; {@code start} is where its id begins. */
    private static final class Header {
        int id;
        long start;
        long dataStart;
        long size;

        long end() {
            return dataStart + size;
        }
    }

    // endregion

    // region copy

    /** Sequential reader of one input with a large window, so most blocks cost no system call. */
    private static final class Reader {
        private final FileChannel channel;
        private final ByteBuffer window = ByteBuffer.allocateDirect(WINDOW_SIZE);
        private long windowStart;
        private int windowLength;

        Reader(FileChannel channel) {
            this.channel = channel;
        }

        void copy(long position, long length, Sink sink) throws IOException {
            while (length > 0) {
                if (position < windowStart || position >= windowStart + windowLength) refill(position);
                final int inWindow = (int) (position - windowStart);
                final int step = (int) Math.min(length, windowLength - inWindow);
                final ByteBuffer slice = window.duplicate();
                slice.limit(inWindow + step).position(inWindow);
                sink.put(slice);
                position += step;
                length -= step;
            }
        }

        private void refill(long position) throws IOException {
            window.clear();
            int total = 0;
            while (window.hasRemaining()) {
                final int read = channel.read(window, position + total);
                if (read < 0) break;
                total += read;
            }
            if (total == 0) throw new IOException("Unexpected end of the input");
            windowStart = position;
            windowLength = total;
        }
    }

    /** Collects output into large writes. */
    private static final class Sink {
        private final FileChannel out;
        private final ByteBuffer buffer = ByteBuffer.allocateDirect(SINK_SIZE);
        long total;

        Sink(FileChannel out) {
            this.out = out;
        }

        void put(byte[] data, int length) throws IOException {
            put(ByteBuffer.wrap(data, 0, length));
        }

        void put(ByteBuffer source) throws IOException {
            total += source.remaining();
            while (source.hasRemaining()) {
                if (!buffer.hasRemaining()) flushBuffer();
                final int step = Math.min(source.remaining(), buffer.remaining());
                final int limit = source.limit();
                source.limit(source.position() + step);
                buffer.put(source);
                source.limit(limit);
            }
        }

        void flush() throws IOException {
            flushBuffer();
        }

        private void flushBuffer() throws IOException {
            buffer.flip();
            while (buffer.hasRemaining()) out.write(buffer);
            buffer.clear();
        }
    }

    // endregion

    private static final class IntList {
        int[] values = new int[256];
        int size;

        void add(int value) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = value;
        }

        int get(int index) {
            return values[index];
        }
    }

    private static final class LongList {
        long[] values = new long[256];
        int size;

        void add(long value) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = value;
        }

        long get(int index) {
            return values[index];
        }
    }

    /** Growable byte writer with EBML helpers. */
    private static final class Buf {
        byte[] data = new byte[256];
        private int length;

        int size() {
            return length;
        }

        void reset() {
            length = 0;
        }

        private void ensure(int more) {
            if (length + more > data.length) data = Arrays.copyOf(data, Math.max(data.length * 2, length + more));
        }

        void u8(int value) {
            ensure(1);
            data[length++] = (byte) value;
        }

        void u64(long value) {
            uintFixed(value, 8);
        }

        void raw(byte[] bytes) {
            raw(bytes, bytes.length);
        }

        void raw(byte[] bytes, int count) {
            ensure(count);
            System.arraycopy(bytes, 0, data, length, count);
            length += count;
        }

        void id(int id) {
            for (int shift = (idLength(id) - 1) * 8; shift >= 0; shift -= 8) u8(id >>> shift);
        }

        /** The shortest size field. */
        void size(long value) {
            final int count = sizeLength(value);
            for (int i = count - 1; i >= 0; i--) {
                int b = (int) (value >>> (8 * i)) & 0xFF;
                if (i == count - 1) b |= 0x80 >> (count - 1);
                u8(b);
            }
        }

        /** An eight byte size field, for the segment. */
        void sizeFixed8(long value) {
            u8(0x01);
            uintFixed(value, 7);
        }

        void uintFixed(long value, int bytes) {
            for (int i = bytes - 1; i >= 0; i--) u8((int) (value >>> (8 * i)));
        }

        void uintBytes(long value) {
            uintFixed(value, uintLength(value));
        }

        void uint(int id, long value) {
            id(id);
            size(uintLength(value));
            uintBytes(value);
        }

        void string(int id, String value) {
            final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            id(id);
            size(bytes.length);
            raw(bytes);
        }

        byte[] toArray() {
            return Arrays.copyOf(data, length);
        }
    }
}
