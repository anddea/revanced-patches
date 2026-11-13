/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 * Written by lavinhoque33, 2026-10-14.
 */

package app.morphe.extension.youtube.patches.general.offline;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

/**
 * Joins one fragmented MP4 video file and one fragmented MP4 audio file (YouTube DASH) into a
 * progressive MP4 without touching a sample: only the sample tables are rebuilt, the sample bytes are
 * copied in large blocks. {@link android.media.MediaMuxer} needs a native call per sample, which
 * took about 30 seconds for a 780 MB video on a phone, this takes a few.
 * <p>
 * Anything that is not a plain, unencrypted, gapless fragmented file with exactly one track of the
 * wanted kind raises {@link UnsupportedInputException}, and the caller falls back to the platform muxer.
 * The output is {@code ftyp}, {@code moov} (so playback can start before the end), then one
 * {@code mdat} with the fragments of both tracks interleaved by time.
 */
final class Mp4Remuxer {
    /** The input is valid, but uses a feature this class does not rebuild. */
    static final class UnsupportedInputException extends IOException {
        UnsupportedInputException(String message) {
            super(message);
        }
    }

    private static final int MAX_BOX_SIZE = 64 << 20;
    private static final int COPY_STEP = 4 << 20;

    private static final int TYPE_MOOV = fourcc("moov");
    private static final int TYPE_MOOF = fourcc("moof");
    private static final int TYPE_MVHD = fourcc("mvhd");
    private static final int TYPE_MVEX = fourcc("mvex");
    private static final int TYPE_TREX = fourcc("trex");
    private static final int TYPE_TRAK = fourcc("trak");
    private static final int TYPE_TKHD = fourcc("tkhd");
    private static final int TYPE_EDTS = fourcc("edts");
    private static final int TYPE_ELST = fourcc("elst");
    private static final int TYPE_SDTP = fourcc("sdtp");
    private static final int TYPE_MDIA = fourcc("mdia");
    private static final int TYPE_MDHD = fourcc("mdhd");
    private static final int TYPE_HDLR = fourcc("hdlr");
    private static final int TYPE_MINF = fourcc("minf");
    private static final int TYPE_STBL = fourcc("stbl");
    private static final int TYPE_STSD = fourcc("stsd");
    private static final int TYPE_TRAF = fourcc("traf");
    private static final int TYPE_TFHD = fourcc("tfhd");
    private static final int TYPE_TFDT = fourcc("tfdt");
    private static final int TYPE_TRUN = fourcc("trun");
    private static final int TYPE_SENC = fourcc("senc");
    private static final int TYPE_SAIZ = fourcc("saiz");
    private static final int TYPE_SAIO = fourcc("saio");
    private static final int TYPE_ENCV = fourcc("encv");
    private static final int TYPE_ENCA = fourcc("enca");
    private static final int HANDLER_VIDEO = fourcc("vide");
    private static final int HANDLER_AUDIO = fourcc("soun");

    private Mp4Remuxer() {
    }

    /**
     * Writes the joined file sequentially to {@code out}, from its current position.
     *
     * @param cancelled Polled between blocks, so a cancelled download stops promptly.
     * @throws UnsupportedInputException If the inputs cannot be remuxed. {@code out} may have been written to.
     */
    static void remux(File video, File audio, FileChannel out, BooleanSupplier cancelled) throws IOException {
        try (RandomAccessFile videoFile = new RandomAccessFile(video, "r");
             RandomAccessFile audioFile = new RandomAccessFile(audio, "r")) {
            Track videoTrack = new Track(videoFile.getChannel(), HANDLER_VIDEO, 1);
            Track audioTrack = new Track(audioFile.getChannel(), HANDLER_AUDIO, 2);
            videoTrack.parse(cancelled);
            audioTrack.parse(cancelled);
            write(videoTrack, audioTrack, out, cancelled);
        }
    }

    // region output

    private static void write(Track video, Track audio, FileChannel out, BooleanSupplier cancelled)
            throws IOException {
        // Interleave the fragments by their start time.
        final int videoChunks = video.chunkLength.size;
        final int audioChunks = audio.chunkLength.size;
        final long[] videoOffsets = new long[videoChunks];
        final long[] audioOffsets = new long[audioChunks];
        final boolean[] order = new boolean[videoChunks + audioChunks]; // true = video
        long dataBytes = 0;
        for (int v = 0, a = 0, n = 0; n < order.length; n++) {
            final boolean takeVideo = v < videoChunks
                    && (a >= audioChunks
                    || video.chunkStart.get(v) * audio.timescale <= audio.chunkStart.get(a) * video.timescale);
            order[n] = takeVideo;
            if (takeVideo) {
                videoOffsets[v] = dataBytes;
                dataBytes += video.chunkLength.get(v++);
            } else {
                audioOffsets[a] = dataBytes;
                dataBytes += audio.chunkLength.get(a++);
            }
        }

        final byte[] ftyp = buildFtyp(video);
        final long movieDuration = Math.max(video.movieDuration(video.movieTimescale), audio.movieDuration(video.movieTimescale));
        long moovSize = buildMoov(video, audio, videoOffsets, audioOffsets, 0, false, movieDuration).length;
        final boolean largeOffsets = ftyp.length + moovSize + 16 + dataBytes > 0xFFFFFFFFL;
        if (largeOffsets) {
            moovSize = buildMoov(video, audio, videoOffsets, audioOffsets, 0, true, movieDuration).length;
        }
        final boolean largeMdat = dataBytes + 8 > 0xFFFFFFFFL;
        final int mdatHeader = largeMdat ? 16 : 8;
        final long dataStart = ftyp.length + moovSize + mdatHeader;
        final byte[] moov = buildMoov(video, audio, videoOffsets, audioOffsets, dataStart, largeOffsets, movieDuration);

        ByteBuffer header = ByteBuffer.allocate(ftyp.length + moov.length + mdatHeader);
        header.put(ftyp).put(moov);
        if (largeMdat) {
            header.putInt(1).putInt(TYPE_MDAT).putLong(dataBytes + 16);
        } else {
            header.putInt((int) (dataBytes + 8)).putInt(TYPE_MDAT);
        }
        header.flip();
        writeFully(out, header);

        long written = 0;
        int v = 0;
        int a = 0;
        for (boolean takeVideo : order) {
            Track track = takeVideo ? video : audio;
            int index = takeVideo ? v++ : a++;
            written += copy(track.channel, track.chunkSource.get(index), track.chunkLength.get(index), out, cancelled);
        }
        if (written != dataBytes) throw new IOException("Copied " + written + " of " + dataBytes + " bytes");
    }

    private static final int TYPE_MDAT = fourcc("mdat");

    private static byte[] buildFtyp(Track video) {
        Buf b = new Buf();
        int box = b.begin("ftyp");
        b.type("isom");
        b.u32(512);
        b.type("isom");
        b.type("iso2");
        b.type(video.sampleEntryType);
        b.type("mp41");
        b.end(box);
        return b.toArray();
    }

    private static byte[] buildMoov(Track video, Track audio, long[] videoOffsets, long[] audioOffsets,
                                    long dataStart, boolean largeOffsets, long movieDuration) throws IOException {
        Buf b = new Buf();
        int moov = b.begin("moov");

        byte[] mvhd = video.mvhd.clone();
        final boolean v1 = mvhd[0] == 1;
        putDuration(mvhd, v1 ? 24 : 16, v1, movieDuration);
        putU32(mvhd, v1 ? 108 : 96, 3);
        int box = b.begin("mvhd");
        b.raw(mvhd);
        b.end(box);

        writeTrak(b, video, videoOffsets, dataStart, largeOffsets, video.movieTimescale);
        writeTrak(b, audio, audioOffsets, dataStart, largeOffsets, video.movieTimescale);
        b.end(moov);
        return b.toArray();
    }

    private static void writeTrak(Buf b, Track t, long[] chunkOffsets, long dataStart, boolean largeOffsets,
                                  long movieTimescale) throws IOException {
        final int trak = b.begin("trak");

        byte[] tkhd = t.tkhd.clone();
        final boolean tkhd1 = tkhd[0] == 1;
        putU32(tkhd, tkhd1 ? 20 : 12, t.outputTrackId);
        putDuration(tkhd, tkhd1 ? 28 : 20, tkhd1, t.movieDuration(movieTimescale));
        int box = b.begin("tkhd");
        b.raw(tkhd);
        b.end(box);

        if (t.editMediaTime >= 0) {
            final long segment = t.movieDuration(movieTimescale);
            box = b.begin("edts");
            int elst = b.begin("elst");
            final boolean large = segment > 0x7FFFFFFFL || t.editMediaTime > 0x7FFFFFFFL;
            b.u32(large ? 0x01000000L : 0);
            b.u32(1);
            if (large) {
                b.u64(segment);
                b.u64(t.editMediaTime);
            } else {
                b.u32(segment);
                b.u32(t.editMediaTime);
            }
            b.u32(t.editRate);
            b.end(elst);
            b.end(box);
        }

        final int mdia = b.begin("mdia");
        byte[] mdhd = t.mdhd.clone();
        final boolean mdhd1 = mdhd[0] == 1;
        putDuration(mdhd, mdhd1 ? 24 : 16, mdhd1, t.mediaDuration);
        box = b.begin("mdhd");
        b.raw(mdhd);
        b.end(box);
        b.raw(t.mdiaOther);

        final int minf = b.begin("minf");
        b.raw(t.minfOther);
        final int stbl = b.begin("stbl");
        b.raw(t.stsd);

        box = b.begin("stts");
        b.u32(0);
        b.u32(t.sttsCount.size);
        for (int i = 0; i < t.sttsCount.size; i++) {
            b.u32(t.sttsCount.get(i));
            b.u32(t.sttsDelta.get(i));
        }
        b.end(box);

        if (t.hasCompositionOffsets) {
            box = b.begin("ctts");
            b.u32(t.hasNegativeOffsets ? 0x01000000L : 0);
            b.u32(t.cttsCount.size);
            for (int i = 0; i < t.cttsCount.size; i++) {
                b.u32(t.cttsCount.get(i));
                b.u32(t.cttsOffset.get(i));
            }
            b.end(box);
        }

        if (t.syncSamples.size != t.sampleCount) {
            box = b.begin("stss");
            b.u32(0);
            b.u32(t.syncSamples.size);
            for (int i = 0; i < t.syncSamples.size; i++) b.u32(t.syncSamples.get(i));
            b.end(box);
        }

        // One chunk per fragment run, so the sample to chunk table has one entry per distinct run length.
        box = b.begin("stsc");
        b.u32(0);
        int entriesAt = b.size();
        b.u32(0);
        int entries = 0;
        int previousSamples = -1;
        for (int i = 0; i < t.chunkSamples.size; i++) {
            int samples = t.chunkSamples.get(i);
            if (samples == previousSamples) continue;
            previousSamples = samples;
            b.u32(i + 1);
            b.u32(samples);
            b.u32(1);
            entries++;
        }
        b.putInt(entriesAt, entries);
        b.end(box);

        box = b.begin("stsz");
        b.u32(0);
        b.u32(0);
        b.u32(t.sizes.size);
        for (int i = 0; i < t.sizes.size; i++) b.u32(t.sizes.get(i));
        b.end(box);

        box = b.begin(largeOffsets ? "co64" : "stco");
        b.u32(0);
        b.u32(chunkOffsets.length);
        for (long offset : chunkOffsets) {
            if (largeOffsets) b.u64(dataStart + offset);
            else b.u32(dataStart + offset);
        }
        b.end(box);

        b.end(stbl);
        b.end(minf);
        b.end(mdia);
        b.end(trak);
    }

    private static long copy(FileChannel in, long position, long length, FileChannel out,
                             BooleanSupplier cancelled) throws IOException {
        long done = 0;
        ByteBuffer buffer = null; // allocated once, only if transferTo fails
        while (done < length) {
            if (cancelled.getAsBoolean()) throw new IOException("Cancelled");
            long step = Math.min(COPY_STEP, length - done);
            long moved = in.transferTo(position + done, step, out);
            if (moved <= 0) {
                // transferTo may move nothing on odd file systems. A plain copy always works.
                if (buffer == null) buffer = ByteBuffer.allocate((int) Math.min(COPY_STEP, 1 << 20));
                buffer.clear().limit((int) Math.min(step, buffer.capacity()));
                int read = in.read(buffer, position + done);
                if (read <= 0) throw new IOException("Unexpected end of the input");
                buffer.flip();
                writeFully(out, buffer);
                moved = read;
            }
            done += moved;
        }
        return done;
    }

    private static void writeFully(FileChannel out, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) out.write(buffer);
    }

    // endregion

    // region input

    private static final class Track {
        final FileChannel channel;
        final int wantedHandler;
        final int outputTrackId;
        final long fileSize;

        // From moov, copied into the output unchanged unless noted.
        byte[] mvhd;
        byte[] tkhd;
        byte[] mdhd;
        byte[] mdiaOther = new byte[0];  // hdlr and the like
        byte[] minfOther = new byte[0];   // vmhd/smhd, dinf
        byte[] stsd;
        int sampleEntryType;
        long movieTimescale;
        long timescale;
        long editMediaTime = -1;          // -1: no edit list
        long editRate;
        long editSegment;
        int sourceTrackId = -1;
        int trexDuration;
        int trexSize;
        int trexFlags;

        // Sample tables rebuilt from the fragments.
        final IntList sizes = new IntList();
        final IntList sttsCount = new IntList();
        final IntList sttsDelta = new IntList();
        final IntList cttsCount = new IntList();
        final IntList cttsOffset = new IntList();
        final IntList syncSamples = new IntList();
        boolean hasCompositionOffsets;
        boolean hasNegativeOffsets;
        long sampleCount;
        long mediaDuration;

        // One entry per run of samples (a "chunk").
        final LongList chunkSource = new LongList();
        final LongList chunkLength = new LongList();
        final LongList chunkStart = new LongList();
        final IntList chunkSamples = new IntList();

        Track(FileChannel channel, int wantedHandler, int outputTrackId) throws IOException {
            this.channel = channel;
            this.wantedHandler = wantedHandler;
            this.outputTrackId = outputTrackId;
            this.fileSize = channel.size();
        }

        /**
         * Duration of the track in {@code scale} units. The edit's start offset is not subtracted:
         * the source files do not do it either, and a too long edit is harmless while a too short one
         * makes players drop the last frames.
         */
        long movieDuration(long scale) {
            // a source edit list's segment_duration is kept (rescaled);
            // fragmented sources usually carry 0 there, which means the whole media.
            if (editMediaTime >= 0 && editSegment > 0) {
                return Math.round((double) editSegment * scale / movieTimescale);
            }
            return Math.round((double) mediaDuration * scale / timescale);
        }

        void parse(BooleanSupplier cancelled) throws IOException {
            long position = 0;
            boolean haveMoov = false;
            ByteBuffer header = ByteBuffer.allocate(16);
            while (position + 8 <= fileSize) {
                header.clear();
                readFully(channel, header, position, (int) Math.min(16, fileSize - position));
                long size = header.getInt(0) & 0xFFFFFFFFL;
                int type = header.getInt(4);
                int headerSize = 8;
                if (size == 1) {
                    if (fileSize - position < 16) throw new UnsupportedInputException("Truncated box header");
                    size = header.getLong(8);
                    headerSize = 16;
                } else if (size == 0) {
                    size = fileSize - position;
                }
                if (size < headerSize || position + size > fileSize) {
                    throw new UnsupportedInputException("Truncated or malformed box");
                }
                if (type == TYPE_MOOV || type == TYPE_MOOF) {
                    if (size > MAX_BOX_SIZE) throw new UnsupportedInputException("Box too large");
                    if (cancelled.getAsBoolean()) throw new IOException("Cancelled");
                    byte[] box = new byte[(int) size];
                    ByteBuffer buffer = ByteBuffer.wrap(box);
                    readFully(channel, buffer, position, box.length);
                    if (type == TYPE_MOOV) {
                        if (haveMoov) throw new UnsupportedInputException("More than one moov");
                        parseMoov(box, headerSize);
                        haveMoov = true;
                    } else {
                        if (!haveMoov) throw new UnsupportedInputException("moof before moov");
                        parseMoof(box, headerSize, position);
                    }
                }
                position += size;
            }
            if (!haveMoov || sampleCount == 0) throw new UnsupportedInputException("No fragments");
            mediaDuration = nextDecodeTime;
        }

        private void parseMoov(byte[] d, int headerSize) throws IOException {
            boolean[] seenMvex = new boolean[1];
            boolean[] seenTrack = new boolean[1];
            forEachBox(d, headerSize, d.length, (type, start, bodyStart, end) -> {
                if (type == TYPE_MVHD) {
                    mvhd = Arrays.copyOfRange(d, bodyStart, end);
                    boolean v1 = mvhd.length > 0 && mvhd[0] == 1;
                    if (mvhd.length < (v1 ? 112 : 100)) throw new UnsupportedInputException("Short mvhd");
                    movieTimescale = u32(mvhd, v1 ? 20 : 12);
                } else if (type == TYPE_MVEX) {
                    seenMvex[0] = true;
                    forEachBox(d, bodyStart, end, (t2, s2, b2, e2) -> {
                        if (t2 == TYPE_TREX) parseTrex(d, b2, e2);
                    });
                } else if (type == TYPE_TRAK && !seenTrack[0]) {
                    if (parseTrak(d, bodyStart, end)) seenTrack[0] = true;
                }
            });
            if (!seenMvex[0]) throw new UnsupportedInputException("Not a fragmented file");
            // mvex may follow trak, so defaults are resolved after all of moov.
            for (int[] trex : trexList) {
                if (seenTrack[0] && trex[0] == sourceTrackId) {
                    trexDuration = trex[2];
                    trexSize = trex[3];
                    trexFlags = trex[4];
                }
            }
            if (!seenTrack[0] || mvhd == null || movieTimescale <= 0) {
                throw new UnsupportedInputException("No usable track");
            }
        }

        private final java.util.ArrayList<int[]> trexList = new java.util.ArrayList<>();

        private void parseTrex(byte[] d, int bodyStart, int end) throws UnsupportedInputException {
            if (end - bodyStart < 24) throw new UnsupportedInputException("Short trex");
            // trackId, description index, duration, size, flags
            trexList.add(new int[]{u32(d, bodyStart + 4), u32(d, bodyStart + 8), u32(d, bodyStart + 12),
                    u32(d, bodyStart + 16), u32(d, bodyStart + 20)});
        }

        /** @return True if this is the track of the wanted kind and it was taken. */
        private boolean parseTrak(byte[] d, int from, int to) throws IOException {
            int[] handler = {0};
            byte[][] tkhdBox = new byte[1][];
            byte[][] mdhdBox = new byte[1][];
            byte[][] stsdBox = new byte[1][];
            Buf mdiaRest = new Buf();
            Buf minfRest = new Buf();
            long[] edit = {-1, 0, 0};

            forEachBox(d, from, to, (type, start, bodyStart, end) -> {
                if (type == TYPE_TKHD) {
                    tkhdBox[0] = Arrays.copyOfRange(d, bodyStart, end);
                } else if (type == TYPE_EDTS) {
                    forEachBox(d, bodyStart, end, (t2, s2, b2, e2) -> {
                        if (t2 != TYPE_ELST) return;
                        boolean v1 = d[b2] == 1;
                        int entry = b2 + 8;
                        if (u32(d, b2 + 4) != 1 || e2 - entry < (v1 ? 20 : 12)) {
                            throw new UnsupportedInputException("Unsupported edit list");
                        }
                        long mediaTime = v1 ? u64(d, entry + 8) : (long) u32(d, entry + 4);
                        if (mediaTime < 0) throw new UnsupportedInputException("Empty edit");
                        edit[0] = mediaTime;
                        edit[2] = v1 ? u64(d, entry) : u32(d, entry) & 0xFFFFFFFFL;
                        edit[1] = u32(d, entry + (v1 ? 16 : 8)) & 0xFFFFFFFFL;
                    });
                } else if (type == TYPE_MDIA) {
                    forEachBox(d, bodyStart, end, (t2, s2, b2, e2) -> {
                        if (t2 == TYPE_MDHD) {
                            mdhdBox[0] = Arrays.copyOfRange(d, b2, e2);
                        } else if (t2 == TYPE_MINF) {
                            forEachBox(d, b2, e2, (t3, s3, b3, e3) -> {
                                if (t3 == TYPE_STBL) {
                                    forEachBox(d, b3, e3, (t4, s4, b4, e4) -> {
                                        if (t4 == TYPE_STSD) stsdBox[0] = Arrays.copyOfRange(d, s4, e4);
                                    });
                                } else {
                                    minfRest.raw(d, s3, e3 - s3);
                                }
                            });
                        } else {
                            if (t2 == TYPE_HDLR) {
                                if (e2 - b2 < 12) throw new UnsupportedInputException("Short hdlr");
                                handler[0] = u32(d, b2 + 8);
                            }
                            mdiaRest.raw(d, s2, e2 - s2);
                        }
                    });
                }
            });
            if (handler[0] != wantedHandler) return false;
            if (tkhdBox[0] == null || mdhdBox[0] == null || stsdBox[0] == null) {
                throw new UnsupportedInputException("Incomplete track");
            }

            tkhd = tkhdBox[0];
            final boolean tkhd1 = tkhd.length > 0 && tkhd[0] == 1;
            if (tkhd.length < (tkhd1 ? 32 : 24)) throw new UnsupportedInputException("Short tkhd");
            sourceTrackId = u32(tkhd, tkhd1 ? 20 : 12);

            mdhd = mdhdBox[0];
            final boolean mdhd1 = mdhd.length > 0 && mdhd[0] == 1;
            if (mdhd.length < (mdhd1 ? 32 : 20)) throw new UnsupportedInputException("Short mdhd");
            timescale = u32(mdhd, mdhd1 ? 20 : 12) & 0xFFFFFFFFL;
            if (timescale <= 0) throw new UnsupportedInputException("Zero timescale");

            stsd = stsdBox[0];
            // box header (8) + version/flags (4) + entry count (4) + entry size (4) + entry type (4)
            if (stsd.length < 24 || u32(stsd, 12) != 1) throw new UnsupportedInputException("Unsupported sample description");
            sampleEntryType = u32(stsd, 20);
            if (sampleEntryType == TYPE_ENCV || sampleEntryType == TYPE_ENCA) {
                throw new UnsupportedInputException("Encrypted track");
            }

            editMediaTime = edit[0];
            editRate = edit[1];
            editSegment = edit[2];
            if (editMediaTime >= 0 && editRate == 0) editRate = 0x00010000;
            mdiaOther = mdiaRest.toArray();
            minfOther = minfRest.toArray();

            return true;
        }

        private long nextDecodeTime;

        private void parseMoof(byte[] d, int headerSize, long moofStart) throws IOException {
            long[] previousEnd = {moofStart};
            boolean[] first = {true};
            forEachBox(d, headerSize, d.length, (type, start, bodyStart, end) -> {
                if (type != TYPE_TRAF) return;
                previousEnd[0] = parseTraf(d, bodyStart, end, moofStart, previousEnd[0], first[0]);
                first[0] = false;
            });
        }

        /** @return The end of the sample data of the fragment. */
        private long parseTraf(byte[] d, int from, int to, long moofStart, long previousEnd, boolean firstTraf)
                throws IOException {
            long[] state = new long[]{
                    -1,          // 0: base data offset
                    0,           // 1: default sample duration
                    0,           // 2: default sample size
                    0,           // 3: default sample flags
                    previousEnd  // 4: cursor, where the next trun's data begins
            };
            boolean[] haveHeader = {false};
            long[] end = {previousEnd};
            forEachBox(d, from, to, (type, start, bodyStart, boxEnd) -> {
                if (type == TYPE_SENC || type == TYPE_SAIZ || type == TYPE_SAIO) {
                    throw new UnsupportedInputException("Encrypted fragments");
                } else if (type == TYPE_SDTP) {
                    // sample dependency flags are not rebuilt.
                    throw new UnsupportedInputException("sdtp is not supported");
                } else if (type == TYPE_TFHD) {
                    int flags = u32(d, bodyStart) & 0xFFFFFF;
                    int p = bodyStart + 4;
                    if (u32(d, p) != sourceTrackId) throw new UnsupportedInputException("Fragment of another track");
                    p += 4;
                    long base;
                    if ((flags & 0x1) != 0) {
                        base = u64(d, p);
                        p += 8;
                    } else if ((flags & 0x20000) != 0 || firstTraf) {
                        base = moofStart;
                    } else {
                        base = previousEnd;
                    }
                    if ((flags & 0x2) != 0) {
                        if (u32(d, p) != 1) throw new UnsupportedInputException("Second sample description");
                        p += 4;
                    }
                    long duration = trexDuration;
                    long size = trexSize;
                    long sampleFlags = trexFlags;
                    if ((flags & 0x8) != 0) { duration = u32(d, p) & 0xFFFFFFFFL; p += 4; }
                    if ((flags & 0x10) != 0) { size = u32(d, p) & 0xFFFFFFFFL; p += 4; }
                    if ((flags & 0x20) != 0) { sampleFlags = u32(d, p) & 0xFFFFFFFFL; p += 4; }
                    if (p > boxEnd) throw new UnsupportedInputException("Short tfhd");
                    state[0] = base;
                    state[1] = duration;
                    state[2] = size;
                    state[3] = sampleFlags;
                    state[4] = base;
                    haveHeader[0] = true;
                } else if (type == TYPE_TFDT) {
                    long time = d[bodyStart] == 1 ? u64(d, bodyStart + 4) : u32(d, bodyStart + 4) & 0xFFFFFFFFL;
                    if (time != nextDecodeTime) throw new UnsupportedInputException("Gap in the timeline");
                } else if (type == TYPE_TRUN) {
                    if (!haveHeader[0]) throw new UnsupportedInputException("trun before tfhd");
                    end[0] = Math.max(end[0], parseTrun(d, bodyStart, boxEnd, state));
                }
            });
            return end[0];
        }

        /** @return The end of the data of the run. */
        private long parseTrun(byte[] d, int bodyStart, int boxEnd, long[] state) throws IOException {
            int flags = u32(d, bodyStart) & 0xFFFFFF;
            final boolean signedOffsets = d[bodyStart] == 1;
            long count = u32(d, bodyStart + 4) & 0xFFFFFFFFL;
            int p = bodyStart + 8;
            long dataPosition = state[4];
            if ((flags & 0x1) != 0) {
                dataPosition = state[0] + u32(d, p); // signed offset
                p += 4;
            }
            long firstFlags = -1;
            if ((flags & 0x4) != 0) {
                firstFlags = u32(d, p) & 0xFFFFFFFFL;
                p += 4;
            }
            final int perSample = ((flags & 0x100) != 0 ? 4 : 0) + ((flags & 0x200) != 0 ? 4 : 0)
                    + ((flags & 0x400) != 0 ? 4 : 0) + ((flags & 0x800) != 0 ? 4 : 0);
            if (count > Integer.MAX_VALUE || p + count * perSample > boxEnd) {
                throw new UnsupportedInputException("Short trun");
            }
            if (count == 0) return dataPosition;

            final long runStart = nextDecodeTime;
            long bytes = 0;
            for (int i = 0; i < count; i++) {
                long duration = state[1];
                long size = state[2];
                long sampleFlags = i == 0 && firstFlags >= 0 ? firstFlags : state[3];
                int offset = 0;
                if ((flags & 0x100) != 0) { duration = u32(d, p) & 0xFFFFFFFFL; p += 4; }
                if ((flags & 0x200) != 0) { size = u32(d, p) & 0xFFFFFFFFL; p += 4; }
                if ((flags & 0x400) != 0) { sampleFlags = u32(d, p) & 0xFFFFFFFFL; p += 4; }
                if ((flags & 0x800) != 0) { offset = u32(d, p); p += 4; }
                if (size > Integer.MAX_VALUE || duration > Integer.MAX_VALUE) {
                    throw new UnsupportedInputException("Sample too large");
                }
                if (offset < 0 && !signedOffsets) throw new UnsupportedInputException("Offset out of range");

                sizes.add((int) size);
                addRun(sttsCount, sttsDelta, (int) duration);
                addRun(cttsCount, cttsOffset, offset);
                if (offset != 0) hasCompositionOffsets = true;
                if (offset < 0) hasNegativeOffsets = true;
                sampleCount++;
                if ((sampleFlags & 0x10000) == 0) syncSamples.add((int) sampleCount);
                bytes += size;
                nextDecodeTime += duration;
            }

            if (dataPosition < 0 || dataPosition + bytes > fileSize) {
                throw new UnsupportedInputException("Sample data outside of the file");
            }
            chunkSource.add(dataPosition);
            chunkLength.add(bytes);
            chunkStart.add(runStart);
            chunkSamples.add((int) count);
            state[4] = dataPosition + bytes;
            return dataPosition + bytes;
        }

        private static void addRun(IntList counts, IntList values, int value) {
            int last = counts.size - 1;
            if (last >= 0 && values.get(last) == value) {
                counts.set(last, counts.get(last) + 1);
            } else {
                counts.add(1);
                values.add(value);
            }
        }
    }

    private interface BoxVisitor {
        /** @param start The first byte of the box, {@code bodyStart} the first byte after its header. */
        void box(int type, int start, int bodyStart, int end) throws IOException;
    }

    private static void forEachBox(byte[] d, int from, int to, BoxVisitor visitor) throws IOException {
        int position = from;
        while (position + 8 <= to) {
            long size = u32(d, position) & 0xFFFFFFFFL;
            int type = u32(d, position + 4);
            int headerSize = 8;
            if (size == 1) {
                if (position + 16 > to) throw new UnsupportedInputException("Truncated box");
                size = u64(d, position + 8);
                headerSize = 16;
            } else if (size == 0) {
                size = to - position;
            }
            if (size < headerSize || position + size > to) throw new UnsupportedInputException("Malformed box");
            visitor.box(type, position, position + headerSize, (int) (position + size));
            position += (int) size;
        }
    }

    private static void readFully(FileChannel channel, ByteBuffer buffer, long position, int length)
            throws IOException {
        buffer.limit(length);
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer, position + buffer.position());
            if (read < 0) throw new UnsupportedInputException("Unexpected end of the file");
        }
    }

    // endregion

    // region helpers

    private static int fourcc(String type) {
        return (type.charAt(0) << 24) | (type.charAt(1) << 16) | (type.charAt(2) << 8) | type.charAt(3);
    }

    private static int u32(byte[] d, int offset) {
        return ((d[offset] & 0xFF) << 24) | ((d[offset + 1] & 0xFF) << 16)
                | ((d[offset + 2] & 0xFF) << 8) | (d[offset + 3] & 0xFF);
    }

    private static long u64(byte[] d, int offset) {
        return ((u32(d, offset) & 0xFFFFFFFFL) << 32) | (u32(d, offset + 4) & 0xFFFFFFFFL);
    }

    private static void putU32(byte[] d, int offset, long value) {
        d[offset] = (byte) (value >>> 24);
        d[offset + 1] = (byte) (value >>> 16);
        d[offset + 2] = (byte) (value >>> 8);
        d[offset + 3] = (byte) value;
    }

    private static void putDuration(byte[] d, int offset, boolean wide, long value) throws UnsupportedInputException {
        if (wide) {
            putU32(d, offset, value >>> 32);
            putU32(d, offset + 4, value);
        } else {
            if (value > 0xFFFFFFFFL) throw new UnsupportedInputException("Duration does not fit");
            putU32(d, offset, value);
        }
    }

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

        void set(int index, int value) {
            values[index] = value;
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

    /** Growable big-endian byte writer with box helpers. */
    private static final class Buf {
        private byte[] data = new byte[4096];
        private int length;

        int size() {
            return length;
        }

        private void ensure(int more) {
            if (length + more > data.length) data = Arrays.copyOf(data, Math.max(data.length * 2, length + more));
        }

        void u32(long value) {
            ensure(4);
            putU32(data, length, value);
            length += 4;
        }

        void u64(long value) {
            u32(value >>> 32);
            u32(value);
        }

        void type(int fourcc) {
            u32(fourcc);
        }

        void type(String type) {
            u32(fourcc(type));
        }

        void raw(byte[] bytes) {
            raw(bytes, 0, bytes.length);
        }

        void raw(byte[] bytes, int offset, int count) {
            ensure(count);
            System.arraycopy(bytes, offset, data, length, count);
            length += count;
        }

        void putInt(int position, int value) {
            putU32(data, position, value);
        }

        int begin(String type) {
            int position = length;
            u32(0);
            type(type);
            return position;
        }

        void end(int position) {
            putInt(position, length - position);
        }

        byte[] toArray() {
            return Arrays.copyOf(data, length);
        }
    }

    // endregion
}
