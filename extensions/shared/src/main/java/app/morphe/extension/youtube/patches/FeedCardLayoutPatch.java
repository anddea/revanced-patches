/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3768
 * https://github.com/MorpheApp/morphe-patches/pull/3769
 * https://github.com/MorpheApp/morphe-patches/pull/3801
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches;

import androidx.annotation.Nullable;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import app.morphe.extension.shared.utils.ByteTrieSearch;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.innertube.FeedCardLayoutOuterClass.CapabilitiesSubscriptionConfig;
import app.morphe.extension.youtube.innertube.FeedCardLayoutOuterClass.SubscriptionConfig;
import app.morphe.extension.youtube.innertube.FeedCardLayoutOuterClass.TemplateConfig;
import app.morphe.extension.youtube.innertube.FeedCardLayoutOuterClass.ThemeSubscriptionConfig;
import app.morphe.extension.youtube.innertube.FeedCardLayoutOuterClass.UriTemplateConfig;

/** Upstream template-cache helpers shared with the two-row video action bar. */
final class FeedCardLayoutPatch {
    private static final Pattern TEMPLATE_SUFFIX_PATTERN = Pattern.compile("-\\w+");
    private static final long TEMPLATE_LOOKUP_INTERVAL_MILLIS = 10_000;
    private static final long TEMPLATE_CONFIRM_INTERVAL_MILLIS = 1000;
    /**
     * Template that the elements are converted to, such as the feed videos.
     * The uri includes the hash of the template version, and the subscriptions of the element write
     * the theme and the capabilities to fields of the model. Both are taken from the elements of the template
     * that the server sends, so they follow the new versions of the template. Until such an element is seen,
     * the uri is taken from the template cache of the app, and the fields are the known ones.
     */
    static final class VideoTemplate {
        final String name;
        final int modelField;
        final ByteTrieSearch search;
        volatile int themeField;
        volatile int capabilitiesField;
        @Nullable
        private volatile String uri;
        /**
         * Whether the uri and the fields were taken from an element.
         */
        private volatile boolean fromElement;
        /**
         * Whether the last lookup did not find the version of the uri in the cache,
         * which is replaced only if the next lookup does not find it either.
         */
        private volatile boolean missingInCache;
        private volatile long cacheLookupTime;
        private volatile long synchronousLookupTime;
        private long checkTime;

        VideoTemplate(String name, int modelField, int themeField, int capabilitiesField) {
            this.name = name;
            this.modelField = modelField;
            this.themeField = themeField;
            this.capabilitiesField = capabilitiesField;
            this.search = new ByteTrieSearch(ByteTrieSearch.convertStringsToBytes(name));
        }

        @Nullable
        String getUri() {
            // The app drops the older versions of the templates from its cache, also while it runs,
            // and the elements of a template version that is no longer cached are not shown.
            // So the version is looked up again once in a while, also the version of an element,
            // which is the one the server sends now, and is kept while the app has it.
            // Until a version is found, the cache is looked up more often, as the elements that are
            // converted meanwhile keep the layout of the server.
            final long now = System.currentTimeMillis();
            if (now - cacheLookupTime > (uri == null || missingInCache
                    ? TEMPLATE_CONFIRM_INTERVAL_MILLIS
                    : TEMPLATE_LOOKUP_INTERVAL_MILLIS)) {
                // The element that is converted now gets the uri of a lookup on its thread,
                // at most as often as before a version is found.
                final boolean synchronous = uri == null
                        && now - synchronousLookupTime > TEMPLATE_LOOKUP_INTERVAL_MILLIS;
                if (synchronous) {
                    synchronousLookupTime = now;
                }
                cacheLookupTime = now;
                // The uri of a template includes the hash of its version, so it's taken from the template
                // cache of the app, which has the templates the app received. The last cached version
                // of the template is used, or none if the template is not cached.
                Runnable lookup = () -> {
                    // The cached versions of the template, from the oldest to the newest.
                    List<String> cached = new ArrayList<>();
                    try {
                        File metadata = new File(Utils.getContext().getFilesDir(), "ElementsResourceCacheMetadata");
                        if (metadata.isFile()) {
                            CodedInputStream input = CodedInputStream.newInstance(
                                    Files.readAllBytes(metadata.toPath()));
                            while (!input.isAtEnd()) {
                                final int tag = input.readTag();
                                if (WireFormat.getTagWireType(tag) != WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                                    input.skipField(tag);
                                    continue;
                                }

                                CodedInputStream entry = CodedInputStream.newInstance(input.readByteArray());
                                while (!entry.isAtEnd()) {
                                    final int entryTag = entry.readTag();
                                    if (WireFormat.getTagFieldNumber(entryTag) == 1
                                            && WireFormat.getTagWireType(entryTag)
                                            == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                                        String key = entry.readString();
                                        if (isTemplate(key, name)) {
                                            cached.add(key);
                                        }
                                    } else {
                                        entry.skipField(entryTag);
                                    }
                                }
                            }
                        }
                    } catch (InvalidProtocolBufferException ex) {
                        // The app rewrites the cache while it runs, so the file can be read while it's
                        // incomplete. The uri found before is kept, and the cache is read again soon.
                        Logger.printDebug(() -> "Template cache is being written", ex);
                        cacheLookupTime = System.currentTimeMillis() - TEMPLATE_LOOKUP_INTERVAL_MILLIS
                                + TEMPLATE_CONFIRM_INTERVAL_MILLIS;
                        return;
                    } catch (Exception ex) {
                        Logger.printException(() -> "getUri failure", ex);
                        return;
                    }
                    // An element of the template can be seen while the cache is read.
                    synchronized (this) {
                        String current = uri;
                        // The app rewrites the cache in the same file while it runs, so a file that is read
                        // while it's rewritten can end before the template without an error. Without the uri,
                        // the elements would keep the layout of the server, so a version that is not found
                        // is replaced only if the next lookup does not find it either.
                        if (current != null && !cached.contains(current) && !missingInCache) {
                            missingInCache = true;
                            Logger.printDebug(() -> "Template not found in the cache: " + current);
                            return;
                        }
                        missingInCache = false;

                        String newest = cached.isEmpty() ? null : cached.get(cached.size() - 1);
                        // The version of an element is kept while it's cached, and while no version is cached,
                        // as the app can use it before it's written to the cache.
                        if (fromElement
                                ? newest == null || cached.contains(current)
                                : Objects.equals(current, newest)) {
                            return;
                        }
                        Logger.printDebug(() -> "Cached template: " + newest);
                        uri = newest;
                        fromElement = false;
                    }
                };
                if (synchronous) {
                    lookup.run();
                } else {
                    Utils.runOnBackgroundThread(lookup);
                }
            }
            return uri;
        }
    }

    static TemplateConfig templateConfig(String uri) {
        return TemplateConfig.newBuilder()
                .setUriTemplateConfig(UriTemplateConfig.newBuilder().setUri(uri))
                .build();
    }

    /**
     * Keeps only the theme and capabilities subscriptions, written to the fields of the new model.
     */
    static SubscriptionConfig subscriptionConfig(SubscriptionConfig config, VideoTemplate template) {
        SubscriptionConfig.Builder builder = SubscriptionConfig.newBuilder();
        if (config.hasThemeSubscriptionConfig()) {
            ThemeSubscriptionConfig theme = config.getThemeSubscriptionConfig();
            builder.setThemeSubscriptionConfig(theme.toBuilder()
                    .setModelField(template.modelField)
                    .setMapping(theme.getMapping().toBuilder().setResultField(template.themeField)));
        }
        if (config.hasCapabilitiesSubscriptionConfig()) {
            CapabilitiesSubscriptionConfig capabilities = config.getCapabilitiesSubscriptionConfig();
            builder.setCapabilitiesSubscriptionConfig(capabilities.toBuilder()
                    .setModelField(template.modelField)
                    .setMapping(capabilities.getMapping().toBuilder().setResultField(template.capabilitiesField)));
        }
        return builder.build();
    }

    private static boolean isTemplate(String uri, String template) {
        if (!uri.startsWith(template)) {
            return false;
        }
        final int hashStart = uri.indexOf('|', template.length());
        return hashStart == template.length() || (hashStart > 0
                && TEMPLATE_SUFFIX_PATTERN.matcher(uri.substring(template.length(), hashStart)).matches());
    }
}
