/*
 * Copyright (C) 2026 lavinhoque33
 * Licensed under the GNU General Public License v3.0.
 */
package app.morphe.extension.music.patches.downloads;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

// Music's device-files library shows only the tags Android's
// media scanner extracts. MatroskaExtractor reports no tags at all, so Opus is repackaged losslessly
// into Ogg Opus (comment tags + METADATA_BLOCK_PICTURE), and MP4 gets an iTunes ilst in its moov.
final class AudioTagger {
    /** Tags written into a download. {@code album} and {@code artwork} (JPEG or PNG) may be null. */
    static final class Tags {
        final String title;
        final String artist;
        final String album;
        final byte[] artwork;

        Tags(String title, String artist, String album, byte[] artwork) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.artwork = artwork;
        }
    }

    private AudioTagger() {}

    static boolean isMp4(String mimeType) {
        return mimeType.startsWith("audio/mp4");
    }

    /** Only these streams can be tagged; other WebM codecs would need a codec-specific Ogg mapping. */
    static boolean canTag(String mimeType) {
        return isMp4(mimeType) || (mimeType.startsWith("audio/webm") && mimeType.contains("opus"));
    }

    static String outputMimeType(String mimeType) {
        return isMp4(mimeType) ? "audio/mp4" : "audio/ogg";
    }

    /** @return The image MIME type, or null if the bytes are neither JPEG nor PNG. */
    static String imageMimeType(byte[] image) {
        if (image.length > 3 && (image[0] & 0xFF) == 0xFF && (image[1] & 0xFF) == 0xD8 && (image[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (image.length > 8 && (image[0] & 0xFF) == 0x89 && image[1] == 'P' && image[2] == 'N' && image[3] == 'G') {
            return "image/png";
        }
        return null;
    }

    /** Writes {@code input} (a downloaded stream of {@code mimeType}) with {@code tags} to {@code output}. */
    static void write(File input, String mimeType, OutputStream output, Tags tags) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(output, 64 * 1024);
        try (RandomAccessFile file = new RandomAccessFile(input, "r")) {
            if (isMp4(mimeType)) {
                Mp4.write(file.getChannel(), out, tags);
            } else {
                OggOpus.write(file.getChannel(), out, tags);
            }
        }
        out.flush();
    }

    // ---------------------------------------------------------------- WebM/Opus -> Ogg Opus (RFC 7845)

    private static final class OggOpus {
        private static final long ID_EBML = 0x1A45DFA3L;
        private static final long ID_SEGMENT = 0x18538067L;
        private static final long ID_TRACKS = 0x1654AE6BL;
        private static final long ID_TRACK_ENTRY = 0xAE;
        private static final long ID_TRACK_NUMBER = 0xD7;
        private static final long ID_CODEC_ID = 0x86;
        private static final long ID_CODEC_PRIVATE = 0x63A2;
        private static final long ID_CLUSTER = 0x1F43B675L;
        private static final long ID_SIMPLE_BLOCK = 0xA3;
        private static final long ID_BLOCK_GROUP = 0xA0;
        private static final long ID_BLOCK = 0xA1;
        private static final long ID_DISCARD_PADDING = 0x75A2;

        private final EbmlReader reader;
        private final OggWriter ogg;
        private final Tags tags;
        private long opusTrack = -1;
        private byte[] opusHead;
        private boolean headersWritten;
        private long samples;
        private long trimmedSamples;

        private OggOpus(EbmlReader reader, OutputStream out, Tags tags) {
            this.reader = reader;
            this.ogg = new OggWriter(out);
            this.tags = tags;
        }

        static void write(FileChannel in, OutputStream out, Tags tags) throws IOException {
            new OggOpus(new EbmlReader(in), out, tags).run(in.size());
        }

        private void run(long fileSize) throws IOException {
            if (reader.readId() != ID_EBML) throw new IOException("Not a WebM file");
            reader.skip(reader.readSize());
            if (reader.readId() != ID_SEGMENT) throw new IOException("WebM has no segment");
            long segmentSize = reader.readSize();
            long segmentEnd = segmentSize < 0 ? fileSize : Math.min(fileSize, reader.position + segmentSize);
            // Master elements whose children matter are read inline, so unknown sizes need no special case.
            while (reader.position < segmentEnd) {
                long id = reader.readId();
                long size = reader.readSize();
                if (id == ID_TRACKS || id == ID_CLUSTER) continue;
                if (size < 0) throw new IOException("WebM element of unknown size: " + Long.toHexString(id));
                if (id == ID_TRACK_ENTRY) {
                    readTrack(reader.readBytes(size));
                } else if (id == ID_SIMPLE_BLOCK) {
                    readBlock(reader.readBytes(size), 0);
                } else if (id == ID_BLOCK_GROUP) {
                    readBlockGroup(reader.readBytes(size));
                } else {
                    reader.skip(size);
                }
            }
            if (!headersWritten) throw new IOException("WebM has no Opus audio");
            // RFC 7845 4.4: end trimming is signalled by a smaller final granule position.
            ogg.finish(samples - trimmedSamples);
        }

        private void readTrack(byte[] entry) throws IOException {
            long number = -1;
            String codec = null;
            byte[] codecPrivate = null;
            EbmlReader children = new EbmlReader(entry);
            while (children.position < entry.length) {
                long id = children.readId();
                long size = children.readSize();
                if (size < 0) throw new IOException("Malformed WebM track");
                if (id == ID_TRACK_NUMBER) number = unsigned(children.readBytes(size));
                else if (id == ID_CODEC_ID) codec = new String(children.readBytes(size), StandardCharsets.US_ASCII).trim();
                else if (id == ID_CODEC_PRIVATE) codecPrivate = children.readBytes(size);
                else children.skip(size);
            }
            if (opusTrack < 0 && "A_OPUS".equals(codec)) {
                if (codecPrivate == null || codecPrivate.length < 19
                        || !new String(codecPrivate, 0, 8, StandardCharsets.US_ASCII).equals("OpusHead")) {
                    throw new IOException("WebM Opus track has no OpusHead");
                }
                opusTrack = number;
                opusHead = codecPrivate;
            }
        }

        private void readBlockGroup(byte[] group) throws IOException {
            byte[] block = null;
            long discardNanoseconds = 0;
            EbmlReader children = new EbmlReader(group);
            while (children.position < group.length) {
                long id = children.readId();
                long size = children.readSize();
                if (size < 0) throw new IOException("Malformed WebM block group");
                if (id == ID_BLOCK) block = children.readBytes(size);
                else if (id == ID_DISCARD_PADDING) discardNanoseconds = signed(children.readBytes(size));
                else children.skip(size);
            }
            if (block != null) readBlock(block, discardNanoseconds);
        }

        private void readBlock(byte[] block, long discardNanoseconds) throws IOException {
            EbmlReader header = new EbmlReader(block);
            long track = header.readSize();
            if (track != opusTrack) return;
            if (!headersWritten) writeHeaders();
            int position = (int) header.position + 2; // relative timecode, unused: granules count samples
            if (position >= block.length) throw new IOException("Truncated WebM block");
            int lacing = (block[position++] >> 1) & 3;
            if (lacing == 0) {
                packet(block, position, block.length - position, discardNanoseconds);
                return;
            }
            int count = (block[position++] & 0xFF) + 1;
            int[] sizes = new int[count];
            long total = 0;
            if (lacing == 1) { // Xiph
                for (int i = 0; i < count - 1; i++) {
                    int size = 0;
                    int value;
                    do {
                        value = block[position++] & 0xFF;
                        size += value;
                    } while (value == 255);
                    sizes[i] = size;
                    total += size;
                }
            } else if (lacing == 3) { // fixed
                int size = (block.length - position) / count;
                for (int i = 0; i < count - 1; i++) sizes[i] = size;
                total = (long) size * (count - 1);
            } else { // EBML: a size, then signed differences
                EbmlReader lace = new EbmlReader(block);
                lace.position = position;
                long size = lace.readSize();
                sizes[0] = (int) size;
                total = size;
                for (int i = 1; i < count - 1; i++) {
                    int start = (int) lace.position;
                    long raw = lace.readSize();
                    int length = (int) lace.position - start;
                    size += raw - ((1L << (7 * length - 1)) - 1);
                    sizes[i] = (int) size;
                    total += size;
                }
                position = (int) lace.position;
            }
            long last = block.length - position - total;
            if (last < 0) throw new IOException("Malformed WebM lacing");
            sizes[count - 1] = (int) last;
            for (int i = 0; i < count; i++) {
                packet(block, position, sizes[i], i == count - 1 ? discardNanoseconds : 0);
                position += sizes[i];
            }
        }

        private void packet(byte[] data, int offset, int length, long discardNanoseconds) throws IOException {
            byte[] packet = new byte[length];
            System.arraycopy(data, offset, packet, 0, length);
            samples += opusSamples(packet);
            if (discardNanoseconds > 0) trimmedSamples += discardNanoseconds * 48_000 / 1_000_000_000L;
            ogg.audio(packet, samples);
        }

        private void writeHeaders() throws IOException {
            if (opusHead == null) throw new IOException("WebM audio precedes its track header");
            ogg.header(opusHead, true);
            ogg.header(opusTags(tags), false);
            headersWritten = true;
        }

        /** Samples at 48 kHz in one Opus packet (RFC 6716 3.1). */
        private static int opusSamples(byte[] packet) throws IOException {
            if (packet.length == 0) throw new IOException("Empty Opus packet");
            int toc = packet[0] & 0xFF;
            int config = toc >> 3;
            int frameSamples;
            if (config < 12) frameSamples = new int[]{480, 960, 1920, 2880}[config & 3];
            else if (config < 16) frameSamples = (config & 1) == 0 ? 480 : 960;
            else frameSamples = 120 << (config & 3);
            int frames = switch (toc & 3) {
                case 0 -> 1;
                case 1, 2 -> 2;
                default -> {
                    if (packet.length < 2) throw new IOException("Truncated Opus packet");
                    yield packet[1] & 0x3F;
                }
            };
            return frameSamples * frames;
        }

        private static byte[] opusTags(Tags tags) {
            ByteArrayOutputStream comments = new ByteArrayOutputStream();
            int[] count = {0};
            Utf8Comment comment = (key, value) -> {
                if (value == null || value.isEmpty()) return;
                byte[] bytes = (key + "=" + value).getBytes(StandardCharsets.UTF_8);
                le32(comments, bytes.length);
                comments.write(bytes, 0, bytes.length);
                count[0]++;
            };
            comment.add("TITLE", tags.title);
            comment.add("ARTIST", tags.artist);
            comment.add("ALBUM", tags.album);
            if (tags.artwork != null) comment.add("METADATA_BLOCK_PICTURE", picture(tags.artwork));
            ByteArrayOutputStream packet = new ByteArrayOutputStream();
            byte[] magic = "OpusTags".getBytes(StandardCharsets.US_ASCII);
            byte[] vendor = "RVX".getBytes(StandardCharsets.US_ASCII);
            packet.write(magic, 0, magic.length);
            le32(packet, vendor.length);
            packet.write(vendor, 0, vendor.length);
            le32(packet, count[0]);
            byte[] body = comments.toByteArray();
            packet.write(body, 0, body.length);
            return packet.toByteArray();
        }

        /** A FLAC picture block (front cover), the form Android's Vorbis comment parser accepts. */
        private static String picture(byte[] image) {
            byte[] mime = imageMimeType(image).getBytes(StandardCharsets.US_ASCII);
            ByteBuffer block = ByteBuffer.allocate(32 + mime.length + image.length);
            block.putInt(3).putInt(mime.length).put(mime).putInt(0) // type, MIME, empty description
                    .putInt(0).putInt(0).putInt(0).putInt(0) // width, height, depth, colours: unknown
                    .putInt(image.length).put(image);
            return Base64.getEncoder().encodeToString(block.array());
        }

        private interface Utf8Comment {
            void add(String key, String value);
        }

        private static long unsigned(byte[] bytes) {
            long value = 0;
            for (byte b : bytes) value = (value << 8) | (b & 0xFF);
            return value;
        }

        private static long signed(byte[] bytes) {
            if (bytes.length == 0) return 0;
            long value = bytes[0]; // sign-extended
            for (int i = 1; i < bytes.length; i++) value = (value << 8) | (bytes[i] & 0xFF);
            return value;
        }
    }

    /** Sequential EBML reader over a file or an in-memory element. */
    private static final class EbmlReader {
        private final InputStream stream;
        private final byte[] bytes;
        long position;

        EbmlReader(FileChannel channel) throws IOException {
            channel.position(0);
            this.stream = new BufferedInputStream(Channels.newInputStream(channel), 64 * 1024);
            this.bytes = null;
        }

        EbmlReader(byte[] bytes) {
            this.stream = null;
            this.bytes = bytes;
        }

        private int read() throws IOException {
            int value;
            if (bytes != null) {
                if (position >= bytes.length) throw new EOFException("Truncated WebM element");
                value = bytes[(int) position] & 0xFF;
            } else {
                value = stream.read();
                if (value < 0) throw new EOFException("Truncated WebM file");
            }
            position++;
            return value;
        }

        /** Element ids keep their length marker. */
        long readId() throws IOException {
            int first = read();
            int length = Integer.numberOfLeadingZeros(first) - 23;
            if (first == 0 || length > 4) throw new IOException("Malformed WebM element id");
            long id = first;
            for (int i = 1; i < length; i++) id = (id << 8) | read();
            return id;
        }

        /** @return The size, or -1 if the element declares an unknown size. */
        long readSize() throws IOException {
            int first = read();
            int length = Integer.numberOfLeadingZeros(first) - 23;
            if (first == 0 || length > 8) throw new IOException("Malformed WebM element size");
            long value = first & (0xFF >> length);
            boolean unknown = value == (0xFF >> length);
            for (int i = 1; i < length; i++) {
                int next = read();
                value = (value << 8) | next;
                unknown &= next == 0xFF;
            }
            return unknown ? -1 : value;
        }

        byte[] readBytes(long size) throws IOException {
            if (size > 64L * 1024 * 1024) throw new IOException("WebM element too large");
            byte[] result = new byte[(int) size];
            if (bytes != null) {
                if (position + size > bytes.length) throw new EOFException("Truncated WebM element");
                System.arraycopy(bytes, (int) position, result, 0, (int) size);
            } else {
                int done = 0;
                while (done < size) {
                    int count = stream.read(result, done, (int) size - done);
                    if (count < 0) throw new EOFException("Truncated WebM file");
                    done += count;
                }
            }
            position += size;
            return result;
        }

        void skip(long size) throws IOException {
            if (bytes != null) {
                if (position + size > bytes.length) throw new EOFException("Truncated WebM element");
            } else {
                long remaining = size;
                while (remaining > 0) {
                    long skipped = stream.skip(remaining);
                    if (skipped <= 0) {
                        if (stream.read() < 0) throw new EOFException("Truncated WebM file");
                        skipped = 1;
                    }
                    remaining -= skipped;
                }
            }
            position += size;
        }
    }

    /** Ogg pages for one logical stream; audio pages hold whole packets of up to ~4 KiB. */
    private static final class OggWriter {
        private static final int[] CRC = new int[256];

        static {
            for (int i = 0; i < 256; i++) {
                int r = i << 24;
                for (int j = 0; j < 8; j++) r = (r & 0x80000000) != 0 ? (r << 1) ^ 0x04C11DB7 : r << 1;
                CRC[i] = r;
            }
        }

        private static final int TARGET_PAGE_BYTES = 4096;

        private final OutputStream out;
        private final int serial = ThreadLocalRandom.current().nextInt();
        private int sequence;
        private final ByteArrayOutputStream pageData = new ByteArrayOutputStream();
        private final int[] lacing = new int[255];
        private int lacingCount;
        private long pageGranule;
        private long lastWrittenGranule;

        OggWriter(OutputStream out) {
            this.out = out;
        }

        /** Header packets get their own pages with granule position 0 and may span pages. */
        void header(byte[] packet, boolean first) throws IOException {
            int segments = packet.length / 255 + 1;
            int offset = 0;
            boolean continued = false;
            while (segments > 0) {
                int pageSegments = Math.min(255, segments);
                int[] pageLacing = new int[pageSegments];
                int pageBytes = 0;
                for (int i = 0; i < pageSegments; i++) {
                    int value = Math.min(255, packet.length - offset - pageBytes);
                    pageLacing[i] = value;
                    pageBytes += value;
                }
                int flags = (continued ? 0x01 : 0) | (first && !continued ? 0x02 : 0);
                page(packet, offset, pageBytes, pageLacing, pageSegments, 0, flags);
                offset += pageBytes;
                segments -= pageSegments;
                continued = true;
            }
        }

        /** @param granule Samples decoded once this packet is complete. */
        void audio(byte[] packet, long granule) throws IOException {
            int segments = packet.length / 255 + 1;
            if (segments > 255) throw new IOException("Opus packet too large for one Ogg page");
            if (lacingCount > 0 && (lacingCount + segments > 255 || pageData.size() + packet.length > TARGET_PAGE_BYTES)) {
                flush(0);
            }
            for (int i = 0; i < segments - 1; i++) lacing[lacingCount++] = 255;
            lacing[lacingCount++] = packet.length % 255;
            pageData.write(packet, 0, packet.length);
            pageGranule = granule;
        }

        void finish(long finalGranule) throws IOException {
            if (lacingCount == 0) throw new IOException("WebM has no Opus packets");
            pageGranule = Math.max(lastWrittenGranule, Math.min(pageGranule, finalGranule));
            flush(0x04);
        }

        private void flush(int flags) throws IOException {
            byte[] data = pageData.toByteArray();
            page(data, 0, data.length, lacing, lacingCount, pageGranule, flags);
            lastWrittenGranule = pageGranule;
            pageData.reset();
            lacingCount = 0;
        }

        private void page(byte[] data, int offset, int length, int[] pageLacing, int segments,
                          long granule, int flags) throws IOException {
            byte[] page = new byte[27 + segments + length];
            page[0] = 'O';
            page[1] = 'g';
            page[2] = 'g';
            page[3] = 'S';
            page[5] = (byte) flags;
            for (int i = 0; i < 8; i++) page[6 + i] = (byte) (granule >>> (8 * i));
            for (int i = 0; i < 4; i++) page[14 + i] = (byte) (serial >>> (8 * i));
            for (int i = 0; i < 4; i++) page[18 + i] = (byte) (sequence >>> (8 * i));
            page[26] = (byte) segments;
            for (int i = 0; i < segments; i++) page[27 + i] = (byte) pageLacing[i];
            System.arraycopy(data, offset, page, 27 + segments, length);
            int crc = 0;
            for (byte b : page) crc = (crc << 8) ^ CRC[((crc >>> 24) ^ b) & 0xFF];
            for (int i = 0; i < 4; i++) page[22 + i] = (byte) (crc >>> (8 * i));
            out.write(page);
            sequence++;
        }
    }

    // ---------------------------------------------------------------- MP4: insert moov/udta/meta/ilst

    private static final class Mp4 {
        private static final int MAX_BOX_IN_MEMORY = 64 * 1024 * 1024;

        static void write(FileChannel in, OutputStream out, Tags tags) throws IOException {
            long fileSize = in.size();
            long position = 0;
            long delta = 0;
            boolean moovWritten = false;
            boolean mediaDataSeen = false;
            while (position < fileSize) {
                Header box = Header.read(in, position, fileSize);
                if (box.is("moov")) {
                    if (moovWritten) throw new IOException("MP4 has two moov boxes");
                    byte[] moov = rebuildMoov(read(in, position, box.size), box.headerSize, tags);
                    delta = moov.length - box.size;
                    // Chunk offsets are absolute: they move only if the media data follows the moov.
                    if (!mediaDataSeen) shiftChunkOffsets(moov, 8, moov.length, delta);
                    out.write(moov);
                    moovWritten = true;
                } else if (box.is("moof") && moovWritten) {
                    byte[] moof = read(in, position, box.size);
                    shiftFragmentBaseOffsets(moof, (int) box.headerSize, moof.length, delta);
                    out.write(moof);
                } else if (box.is("mfra") && moovWritten) {
                    // Optional random-access index of absolute moof offsets; sidx/moov still index the file.
                } else {
                    if (box.is("mdat") || box.is("moof")) mediaDataSeen = true;
                    copy(in, position, box.size, out);
                }
                position += box.size;
            }
            if (!moovWritten) throw new IOException("MP4 has no moov");
        }

        private static byte[] rebuildMoov(byte[] moov, long headerSize, Tags tags) throws IOException {
            ByteArrayOutputStream children = new ByteArrayOutputStream(moov.length + 1024);
            int position = (int) headerSize;
            while (position < moov.length) {
                Header child = Header.read(moov, position, moov.length);
                if (!child.is("udta")) children.write(moov, position, (int) child.size);
                position += (int) child.size;
            }
            byte[] udta = box("udta", fullBox("meta", 0, concat(handler(), box("ilst", ilst(tags)))));
            children.write(udta, 0, udta.length);
            return box("moov", children.toByteArray());
        }

        private static byte[] ilst(Tags tags) {
            ByteArrayOutputStream items = new ByteArrayOutputStream();
            text(items, "\u00A9nam", tags.title);
            text(items, "\u00A9ART", tags.artist);
            text(items, "\u00A9alb", tags.album);
            if (tags.artwork != null) {
                int type = "image/png".equals(imageMimeType(tags.artwork)) ? 14 : 13;
                byte[] covr = box("covr", data(type, tags.artwork));
                items.write(covr, 0, covr.length);
            }
            return items.toByteArray();
        }

        private static void text(ByteArrayOutputStream items, String type, String value) {
            if (value == null || value.isEmpty()) return;
            byte[] item = box(type, data(1, value.getBytes(StandardCharsets.UTF_8)));
            items.write(item, 0, item.length);
        }

        private static byte[] data(int type, byte[] payload) {
            ByteBuffer data = ByteBuffer.allocate(8 + payload.length);
            data.putInt(type).putInt(0).put(payload); // well-known type, default locale
            return box("data", data.array());
        }

        private static byte[] handler() {
            ByteBuffer hdlr = ByteBuffer.allocate(21);
            hdlr.putInt(0).put(fourcc("mdir")).put(fourcc("appl")).putInt(0).putInt(0).put((byte) 0);
            return fullBox("hdlr", 0, hdlr.array());
        }

        /** Walks moov/trak/mdia/minf/stbl and shifts every stco/co64 chunk offset. */
        private static void shiftChunkOffsets(byte[] d, int from, int to, long delta) throws IOException {
            int position = from;
            while (position < to) {
                Header box = Header.read(d, position, to);
                int body = position + (int) box.headerSize;
                int end = position + (int) box.size;
                if (box.is("trak") || box.is("mdia") || box.is("minf") || box.is("stbl")) {
                    shiftChunkOffsets(d, body, end, delta);
                } else if (box.is("stco")) {
                    int count = u32(d, body + 4);
                    for (int i = 0; i < count; i++) {
                        int at = body + 8 + 4 * i;
                        long value = (u32(d, at) & 0xFFFFFFFFL) + delta;
                        if (value > 0xFFFFFFFFL) throw new IOException("MP4 chunk offset overflow");
                        putU32(d, at, value);
                    }
                } else if (box.is("co64")) {
                    int count = u32(d, body + 4);
                    for (int i = 0; i < count; i++) {
                        int at = body + 8 + 8 * i;
                        long value = ((u32(d, at) & 0xFFFFFFFFL) << 32 | (u32(d, at + 4) & 0xFFFFFFFFL)) + delta;
                        putU32(d, at, value >>> 32);
                        putU32(d, at + 4, value);
                    }
                }
                position = end;
            }
        }

        /** Fragments normally address data relative to their moof; explicit base offsets are absolute. */
        private static void shiftFragmentBaseOffsets(byte[] d, int from, int to, long delta) throws IOException {
            int position = from;
            while (position < to) {
                Header box = Header.read(d, position, to);
                int body = position + (int) box.headerSize;
                if (box.is("traf")) {
                    shiftFragmentBaseOffsets(d, body, position + (int) box.size, delta);
                } else if (box.is("tfhd") && (u32(d, body) & 0x000001) != 0) {
                    int at = body + 8; // version/flags, track_ID
                    long value = ((u32(d, at) & 0xFFFFFFFFL) << 32 | (u32(d, at + 4) & 0xFFFFFFFFL)) + delta;
                    putU32(d, at, value >>> 32);
                    putU32(d, at + 4, value);
                }
                position += (int) box.size;
            }
        }

        private static byte[] read(FileChannel in, long position, long size) throws IOException {
            if (size > MAX_BOX_IN_MEMORY) throw new IOException("MP4 box too large");
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            while (buffer.hasRemaining()) {
                if (in.read(buffer, position + buffer.position()) < 0) throw new EOFException("Truncated MP4");
            }
            return buffer.array();
        }

        private static void copy(FileChannel in, long position, long size, OutputStream out) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(size, 256 * 1024));
            long done = 0;
            while (done < size) {
                buffer.clear();
                buffer.limit((int) Math.min(buffer.capacity(), size - done));
                int count = in.read(buffer, position + done);
                if (count < 0) throw new EOFException("Truncated MP4");
                out.write(buffer.array(), 0, count);
                done += count;
            }
        }

        private static byte[] box(String type, byte[] body) {
            ByteBuffer box = ByteBuffer.allocate(8 + body.length);
            box.putInt(8 + body.length).put(fourcc(type)).put(body);
            return box.array();
        }

        private static byte[] fullBox(String type, int versionAndFlags, byte[] body) {
            ByteBuffer content = ByteBuffer.allocate(4 + body.length);
            content.putInt(versionAndFlags).put(body);
            return box(type, content.array());
        }

        private static byte[] concat(byte[] a, byte[] b) {
            byte[] result = new byte[a.length + b.length];
            System.arraycopy(a, 0, result, 0, a.length);
            System.arraycopy(b, 0, result, a.length, b.length);
            return result;
        }

        /** Box types are ISO-8859-1, so the iTunes copyright sign is the single byte 0xA9. */
        private static byte[] fourcc(String type) {
            return type.getBytes(StandardCharsets.ISO_8859_1);
        }

        private record Header(String type, long headerSize, long size) {
            boolean is(String other) {
                return type.equals(other);
            }

            static Header read(FileChannel in, long position, long fileSize) throws IOException {
                ByteBuffer header = ByteBuffer.allocate(16);
                header.limit((int) Math.min(16, fileSize - position));
                while (header.hasRemaining()) {
                    if (in.read(header, position + header.position()) < 0) break;
                }
                return parse(header.array(), 0, header.position(), fileSize - position);
            }

            static Header read(byte[] d, int position, int end) throws IOException {
                return parse(d, position, Math.min(16, end - position), end - position);
            }

            private static Header parse(byte[] d, int offset, int available, long remaining) throws IOException {
                if (available < 8) throw new IOException("Truncated MP4 box");
                long size = u32(d, offset) & 0xFFFFFFFFL;
                String type = new String(d, offset + 4, 4, StandardCharsets.ISO_8859_1);
                long headerSize = 8;
                if (size == 1) {
                    if (available < 16) throw new IOException("Truncated MP4 box");
                    size = (u32(d, offset + 8) & 0xFFFFFFFFL) << 32 | (u32(d, offset + 12) & 0xFFFFFFFFL);
                    headerSize = 16;
                } else if (size == 0) {
                    size = remaining;
                }
                if (size < headerSize || size > remaining) throw new IOException("Malformed MP4 box " + type);
                return new Header(type, headerSize, size);
            }
        }
    }

    private static int u32(byte[] d, int offset) {
        return ((d[offset] & 0xFF) << 24) | ((d[offset + 1] & 0xFF) << 16)
                | ((d[offset + 2] & 0xFF) << 8) | (d[offset + 3] & 0xFF);
    }

    private static void putU32(byte[] d, int offset, long value) {
        d[offset] = (byte) (value >>> 24);
        d[offset + 1] = (byte) (value >>> 16);
        d[offset + 2] = (byte) (value >>> 8);
        d[offset + 3] = (byte) value;
    }

    private static void le32(ByteArrayOutputStream out, int value) {
        out.write(value);
        out.write(value >>> 8);
        out.write(value >>> 16);
        out.write(value >>> 24);
    }
}
