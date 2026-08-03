/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.music.patches.components;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.FilterGroup.FilterGroupResult;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public final class ActionButtonsFilter extends Filter {
    public interface ButtonProtoBufferInterface {
        byte[] patch_getBuffer();
    }

    public interface LithoGetBufferContainerInterface {
        Object patch_getContainer();
    }

    private enum ActionButton {
        LIKE_DISLIKE(Settings.HIDE_ACTION_BUTTON_LIKE_DISLIKE),
        DOWNLOAD(Settings.HIDE_ACTION_BUTTON_DOWNLOAD),
        COMMENTS(Settings.HIDE_ACTION_BUTTON_COMMENT),
        LIVE_CHAT_REPLAY(Settings.HIDE_ACTION_BUTTON_LIVE_CHAT_REPLAY),
        DETAILS(Settings.HIDE_ACTION_BUTTON_DETAILS),
        LYRICS(Settings.HIDE_ACTION_BUTTON_LYRICS),
        SHARE(Settings.HIDE_ACTION_BUTTON_SHARE),
        RADIO(Settings.HIDE_ACTION_BUTTON_RADIO),
        SAVE(Settings.HIDE_ACTION_BUTTON_ADD_TO_PLAYLIST);

        final BooleanSetting setting;

        ActionButton(BooleanSetting setting) {
            this.setting = setting;
        }
    }

    private static final String VIDEO_ACTION_BAR_PATH_PREFIX = "video_action_bar.";
    private static final String LAZILY_CONVERTED_ELEMENT = "LazilyConvertedElement";
    private static final String VIDEO_ACTION_BUTTON_WRAPPER_PREFIX = "video_action_button_with_vm_input.e";
    private static final String LIKE_BUTTON_MARKER = "like_button";
    private static final String DISLIKE_BUTTON_MARKER = "dislike_button";
    private static final String SEGMENTED_LIKE_DISLIKE_MARKER = "segmented_like_dislike_button";
    private static final String DOWNLOAD_MARKER = "download_button";
    private static final String MUSIC_DOWNLOAD_MARKER = "music_download_button";
    private static final String DOWNLOAD_PROTO_MARKER = "offlinelist";
    private static final String COMMENTS_MARKER = "music-comment-panel";
    private static final String LIVE_CHAT_REPLAY_MARKER = "live-chat-item-section";
    private static final String DETAILS_MARKER = "video-description-ep-identifier";
    private static final String LYRICS_MARKER = "music_watch_lyrics_panel";
    private static final String SHARE_MARKER = "timestamp_share_switch_button_entity_key";
    private static final String RADIO_MARKER = "RDAMVM";
    private static final String SAVE_MARKER = "yt_outline_experimental_playlist_add_vd_theme_24";
    private static final String SAVE_MARKER_ALT = "yt_outline_list_add";
    private static final String COMMENTS_ICON = "_text_bubble_";
    private static final String LYRICS_ICON = "_quote_";
    private static final String THUMB_UP_ICON = "_thumb_up_";
    private static final String THUMB_DOWN_ICON = "_thumb_down_";
    private static final int ICON_SCAN_HEAD_LIMIT = 500;
    private static final int MAX_FLAT_BUFFER_TREE_DEPTH = 8;
    private static final int MAX_FLAT_BUFFER_TREE_NODES = 128;

    private final StringFilterGroup actionBarRule;
    private final StringFilterGroup genericActionButtonRule;
    private final StringFilterGroup bufferFilterPathRule;
    private final ByteArrayFilterGroup buttonContentEnd = new ByteArrayFilterGroup(
            null,
            "capabilities|",
            "sans-serif-regular",
            "yt_outline_overflow_vertical_white_24"
    );
    private final ByteArrayFilterGroupList bufferButtonsGroupList = new ByteArrayFilterGroupList();

    public ActionButtonsFilter() {
        actionBarRule = new StringFilterGroup(
                null,
                VIDEO_ACTION_BAR_PATH_PREFIX
        );
        addIdentifierCallbacks(actionBarRule);

        genericActionButtonRule = new StringFilterGroup(
                null,
                VIDEO_ACTION_BUTTON_WRAPPER_PREFIX
        );
        bufferFilterPathRule = new StringFilterGroup(
                null,
                "|ContainerType|button."
        );
        final StringFilterGroup downloadButton = new StringFilterGroup(
                Settings.HIDE_ACTION_BUTTON_DOWNLOAD,
                "music_download_button."
        );
        final StringFilterGroup likeDislikeContainer = new StringFilterGroup(
                Settings.HIDE_ACTION_BUTTON_LIKE_DISLIKE,
                "segmented_like_dislike_button."
        );
        final StringFilterGroup songVideoButton = new StringFilterGroup(
                Settings.HIDE_ACTION_BUTTON_SONG_VIDEO,
                "music_audio_video_button."
        );
        addPathCallbacks(
                genericActionButtonRule,
                bufferFilterPathRule,
                downloadButton,
                likeDislikeContainer,
                songVideoButton
        );

        bufferButtonsGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_COMMENT,
                        "yt_outline_experimental_text_bubble_vd_theme_24",
                        "yt_outline_message_bubble"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_LIVE_CHAT_REPLAY,
                        "yt_outline_experimental_bubble_stack_vd_theme_24",
                        LIVE_CHAT_REPLAY_MARKER
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_DETAILS,
                        "yt_outline_experimental_text_align_left_vd_theme_24",
                        DETAILS_MARKER
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_ADD_TO_PLAYLIST,
                        "yt_outline_experimental_playlist_add_vd_theme_24",
                        "yt_outline_list_add"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_LYRICS,
                        "yt_outline_experimental_quote_vd_theme_24"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_SHARE,
                        "yt_outline_experimental_share_vd_theme_24",
                        "yt_outline_share"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_RADIO,
                        "yt_outline_experimental_mix_vd_theme_24",
                        "yt_outline_youtube_mix"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_DISABLED,
                        "button_container_disabled"
                )
        );
    }

    private boolean isEveryFilterGroupEnabled() {
        for (StringFilterGroup group : pathCallbacks)
            if (!group.isEnabled()) return false;

        for (ByteArrayFilterGroup group : bufferButtonsGroupList)
            if (!group.isEnabled()) return false;

        return true;
    }

    /**
     * Checks only the button-local portion of the component buffer.
     *
     * <p>Music appends shared component data after the button-local data. Matching an icon in the
     * shared data would make one enabled hide setting match unrelated buttons.</p>
     */
    private boolean matchesButtonContent(byte[] buffer) {
        final FilterGroupResult buttonMatch = bufferButtonsGroupList.check(buffer);
        if (!buttonMatch.isFiltered()) {
            return false;
        }

        final int contentEnd = buttonContentEnd.check(buffer).getMatchedIndex();
        return contentEnd < 0 || buttonMatch.getMatchedIndex() < contentEnd;
    }

    public static void onLazilyConvertedElementLoaded(@NonNull List<Object> treeNodeResultList,
                                                      @NonNull String identifier) {
        try {
            if (!identifier.startsWith(VIDEO_ACTION_BAR_PATH_PREFIX)) {
                return;
            }
            if (treeNodeResultList.isEmpty()) {
                Logger.printDebug(() -> "Action bar tree hook ignored empty list, identifier: " + identifier);
                return;
            }

            final String firstElement = treeNodeResultList.get(0).toString();
            if (!LAZILY_CONVERTED_ELEMENT.equals(firstElement)) {
                Logger.printDebug(() -> "Action bar tree hook ignored non-lazy list, identifier: "
                        + identifier + ", firstElement: " + firstElement);
                return;
            }

            int matchedCount = 0;
            int removedCount = 0;
            int missingProtoCount = 0;
            for (int i = treeNodeResultList.size() - 1; i >= 0; i--) {
                byte[] buttonProto = extractButtonProto(treeNodeResultList.get(i));
                if (buttonProto == null) {
                    missingProtoCount++;
                    continue;
                }
                matchedCount++;
                ActionButton button = classify(buttonProto);
                if (button == null) continue;
                if (!button.setting.get()) continue;
                if (i == 0 && button == ActionButton.LIKE_DISLIKE) continue;
                treeNodeResultList.remove(i);
                removedCount++;
            }
            final int matchedCountFinal = matchedCount;
            final int removedCountFinal = removedCount;
            final int missingProtoCountFinal = missingProtoCount;
            Logger.printDebug(() -> "Action bar tree hook processed identifier: " + identifier
                    + ", matchedProto: " + matchedCountFinal
                    + ", missingProto: " + missingProtoCountFinal
                    + ", removed: " + removedCountFinal);
        } catch (Exception ex) {
            Logger.printException(() -> "onLazilyConvertedElementLoaded failure", ex);
        }
    }

    @Nullable
    private static byte[] extractButtonProto(@Nullable Object item) {
        if (item instanceof ButtonProtoBufferInterface holder) {
            return holder.patch_getBuffer();
        }
        if (item instanceof LithoGetBufferContainerInterface bufferInterface) {
            return extractButtonProto(bufferInterface.patch_getContainer());
        }

        return extractFlatBufferPayload(item);
    }

    @Nullable
    private static byte[] copyByteBuffer(@Nullable ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }

        ByteBuffer duplicate = buffer.duplicate();
        byte[] copy = new byte[duplicate.remaining()];
        duplicate.get(copy);
        return copy;
    }

    @Nullable
    private static byte[] extractFlatBufferPayload(@Nullable Object item) {
        if (item == null) {
            return null;
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            copyFlatBufferElementTree(item, output, new AtomicInteger(), 0);
            return output.size() == 0 ? null : output.toByteArray();
        } catch (Exception ignored) {
            // Not a FlatBuffer-backed element, or not the wrapper shape used by YouTube Music.
        }
        return null;
    }

    private static void copyFlatBufferElementTree(@Nullable Object item, @NonNull ByteArrayOutputStream output,
                                                  @NonNull AtomicInteger visitedCount, int depth) throws Exception {
        if (item == null
                || depth > MAX_FLAT_BUFFER_TREE_DEPTH
                || visitedCount.incrementAndGet() > MAX_FLAT_BUFFER_TREE_NODES) {
            return;
        }

        FlatBufferElementReader reader = FlatBufferElementReader.create(item.getClass());
        if (reader != null) {
            try {
                reader.copyLocal(item, output);
            } catch (Exception ignored) {
                // Keep walking, some obfuscated accessors are not safe for every element instance.
            }
            int childCount;
            try {
                childCount = reader.getChildCount(item);
            } catch (Exception ex) {
                return;
            }
            for (int i = 0; i < childCount; i++) {
                try {
                    copyFlatBufferElementTree(reader.getChild(item, i), output, visitedCount, depth + 1);
                } catch (Exception ignored) {
                    // Skip malformed child accessors, other children may still carry the marker.
                }
            }
            return;
        }

        for (Field field : item.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object table = field.get(item);
            byte[] payload = copyFlatBufferModelPayload(table);
            if (payload != null) {
                output.write(payload);
            }
        }
    }

    @Nullable
    private static Object unwrapOptional(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            Method fMethod = null;
            Method bMethod = null;
            for (Method method : obj.getClass().getMethods()) {
                if (method.getParameterCount() == 0) {
                    if (method.getName().equals("f") && method.getReturnType() == boolean.class) {
                        fMethod = method;
                    } else if (method.getName().equals("b") && method.getReturnType() == Object.class) {
                        bMethod = method;
                    }
                }
            }
            if (fMethod != null && bMethod != null) {
                fMethod.setAccessible(true);
                bMethod.setAccessible(true);
                if (Boolean.TRUE.equals(fMethod.invoke(obj))) {
                    return bMethod.invoke(obj);
                }
            }
        } catch (Exception ignored) {
        }
        return obj;
    }

    @Nullable
    private static byte[] copyFlatBufferModelPayload(@Nullable Object rootTable) {
        return copyFlatBufferModelPayload(rootTable, new HashSet<>(), 0);
    }

    @Nullable
    private static byte[] copyFlatBufferModelPayload(@Nullable Object rootTable, @NonNull Set<Object> visited, int depth) {
        if (rootTable == null || depth > 4) {
            return null;
        }
        rootTable = unwrapOptional(rootTable);
        if (rootTable == null || !visited.add(rootTable)) {
            return null;
        }

        Class<?> clazz = rootTable.getClass();
        String className = clazz.getName();
        if (className.startsWith("java.") || className.startsWith("android.")) {
            if (!ByteBuffer.class.isAssignableFrom(clazz)) {
                return null;
            }
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Copy strings first (such as component identifier keys).
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType() == String.class) {
                try {
                    method.setAccessible(true);
                    String value = (String) method.invoke(rootTable);
                    if (value != null && !value.isEmpty()) {
                        output.write(value.getBytes(StandardCharsets.ISO_8859_1));
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        // Extract any direct ByteBuffer methods
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType() == ByteBuffer.class) {
                try {
                    method.setAccessible(true);
                    ByteBuffer buffer = (ByteBuffer) method.invoke(rootTable);
                    byte[] payload = copyByteBuffer(buffer);
                    if (payload != null) {
                        output.write(payload);
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        // Extract any methods returning sub-tables that have direct ByteBuffer methods
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 
                    && !method.getReturnType().isPrimitive() 
                    && method.getReturnType() != String.class 
                    && method.getReturnType() != void.class 
                    && method.getReturnType().getSimpleName().length() <= 3) {
                try {
                    boolean hasByteBufferMethod = false;
                    for (Method subMethod : method.getReturnType().getDeclaredMethods()) {
                        if (subMethod.getParameterCount() == 0 && subMethod.getReturnType() == ByteBuffer.class) {
                            hasByteBufferMethod = true;
                            break;
                        }
                    }
                    if (hasByteBufferMethod) {
                        method.setAccessible(true);
                        Object subTable = method.invoke(rootTable);
                        if (subTable != null) {
                            byte[] payload = copyFlatBufferModelPayload(subTable, visited, depth + 1);
                            if (payload != null) {
                                output.write(payload);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        for (Method modelMethod : clazz.getDeclaredMethods()) {
            if (modelMethod.getParameterCount() != 0
                    || modelMethod.getReturnType().isPrimitive()
                    || modelMethod.getReturnType() == String.class
                    || modelMethod.getReturnType() == ByteBuffer.class) {
                continue;
            }

            ModelPayloadReader reader = ModelPayloadReader.create(modelMethod.getReturnType());
            if (reader == null) {
                continue;
            }

            try {
                modelMethod.setAccessible(true);
                Object modelTable = modelMethod.invoke(rootTable);
                if (modelTable != null) {
                    byte[] payload = reader.copy(modelTable);
                    if (payload != null) {
                        output.write(payload);
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // Recurse into fields
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(rootTable);
                if (fieldValue != null && fieldValue != rootTable) {
                    byte[] payload = copyFlatBufferModelPayload(fieldValue, visited, depth + 1);
                    if (payload != null) {
                        output.write(payload);
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return output.size() == 0 ? null : output.toByteArray();
    }

    private record ModelPayloadReader(Method countMethod, Method childMethod, Method payloadMethod,
                                      Class<?> childType) {
            private static final int MAX_MODEL_PAYLOADS = 64;

        @Nullable
            static ModelPayloadReader create(@NonNull Class<?> modelType) {
                try {
                    Method countMethod = null;
                    Method childMethod = null;
                    for (Method method : modelType.getDeclaredMethods()) {
                        if (method.getParameterCount() == 0 && method.getReturnType() == int.class) {
                            if (!method.getName().equals("hashCode")) {
                                countMethod = method;
                            }
                        } else if (method.getParameterCount() == 2
                                && method.getParameterTypes()[1] == int.class
                                && method.getReturnType() == method.getParameterTypes()[0]) {
                            childMethod = method;
                        }
                    }
                    if (countMethod == null || childMethod == null) {
                        return null;
                    }

                    Class<?> childType = childMethod.getReturnType();
                    Method payloadMethod = null;
                    for (Method method : childType.getDeclaredMethods()) {
                        if (method.getParameterCount() == 0 && method.getReturnType() == ByteBuffer.class) {
                            payloadMethod = method;
                            break;
                        }
                    }
                    if (payloadMethod == null) {
                        return null;
                    }

                    countMethod.setAccessible(true);
                    childMethod.setAccessible(true);
                    payloadMethod.setAccessible(true);
                    return new ModelPayloadReader(countMethod, childMethod, payloadMethod, childType);
                } catch (Exception ex) {
                    return null;
                }
            }

            @Nullable
            byte[] copy(@NonNull Object modelTable) throws Exception {
                Integer countVal = (Integer) countMethod.invoke(modelTable);
                int count = countVal != null ? countVal : 0;
                if (count <= 0 || count > MAX_MODEL_PAYLOADS) {
                    return null;
                }

                Constructor<?> constructor = childType.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object child = constructor.newInstance();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (int i = 0; i < count; i++) {
                    Object payloadTable = childMethod.invoke(modelTable, child, i);
                    if (payloadTable == null) {
                        continue;
                    }
                    ByteBuffer buffer = (ByteBuffer) payloadMethod.invoke(payloadTable);
                    byte[] payload = copyByteBuffer(buffer);
                    if (payload != null) {
                        output.write(payload);
                    }
                }
                return output.size() == 0 ? null : output.toByteArray();
            }
        }

    private record FlatBufferElementReader(Method countMethod, Method childMethod, Method[] stringMethods) {

        @Nullable
            static FlatBufferElementReader create(@NonNull Class<?> elementType) {
                try {
                    Method countMethod = null;
                    Method childMethod = null;
                    int stringMethodCount = 0;
                    Method[] stringMethods = new Method[elementType.getDeclaredMethods().length];
                    for (Method method : elementType.getDeclaredMethods()) {
                        if (method.getParameterCount() == 0) {
                            if (method.getReturnType() == int.class) {
                                if (!method.getName().equals("hashCode")) {
                                    countMethod = method;
                                }
                            } else if (method.getReturnType() == String.class) {
                                stringMethods[stringMethodCount++] = method;
                            }
                        } else if (method.getParameterCount() == 1
                                && method.getParameterTypes()[0] == int.class
                                && method.getReturnType() != Object.class
                                && method.getReturnType().isAssignableFrom(elementType)) {
                            childMethod = method;
                        }
                    }
                    if (countMethod == null || childMethod == null) {
                        return null;
                    }

                    countMethod.setAccessible(true);
                    childMethod.setAccessible(true);
                    Method[] collectedStringMethods = new Method[stringMethodCount];
                    for (int i = 0; i < stringMethodCount; i++) {
                        collectedStringMethods[i] = stringMethods[i];
                        collectedStringMethods[i].setAccessible(true);
                    }
                    return new FlatBufferElementReader(countMethod, childMethod, collectedStringMethods);
                } catch (Exception ex) {
                    return null;
                }
            }

            void copyLocal(@NonNull Object element, @NonNull ByteArrayOutputStream output) throws Exception {
                for (Method method : element.getClass().getDeclaredMethods()) {
                    if (method.getParameterCount() == 0
                            && !method.getReturnType().isPrimitive()
                            && method.getReturnType() != String.class
                            && method.getReturnType() != void.class
                            && method.getReturnType().getSimpleName().length() <= 3) {
                        try {
                            method.setAccessible(true);
                            method.invoke(element);
                        } catch (Throwable ignored) {
                        }
                    }
                }

                for (Method method : stringMethods) {
                    Object value = method.invoke(element);
                    if (value instanceof String string && !string.isEmpty()) {
                        output.write(string.getBytes(StandardCharsets.ISO_8859_1));
                    }
                }

                for (Field field : element.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    Object table = field.get(element);
                    byte[] payload = copyFlatBufferModelPayload(table);
                    if (payload != null) {
                        output.write(payload);
                    }
                }
            }

            int getChildCount(@NonNull Object element) throws Exception {
                Integer childCountVal = (Integer) countMethod.invoke(element);
                int childCount = childCountVal != null ? childCountVal : 0;
                return Math.max(0, Math.min(childCount, MAX_FLAT_BUFFER_TREE_NODES));
            }

            @Nullable
            Object getChild(@NonNull Object element, int index) throws Exception {
                return childMethod.invoke(element, index);
            }
        }

    @Nullable
    private static ActionButton classify(byte[] buffer) {
        String contents = new String(buffer, StandardCharsets.ISO_8859_1);
        if (contents.contains(SEGMENTED_LIKE_DISLIKE_MARKER)
                || contents.contains(LIKE_BUTTON_MARKER)
                || contents.contains(DISLIKE_BUTTON_MARKER)) {
            return ActionButton.LIKE_DISLIKE;
        }
        if (contents.contains(MUSIC_DOWNLOAD_MARKER)
                || contents.contains(DOWNLOAD_MARKER)
                || contents.contains(DOWNLOAD_PROTO_MARKER)) {
            return ActionButton.DOWNLOAD;
        }
        if (contents.contains(COMMENTS_MARKER)) return ActionButton.COMMENTS;
        if (contents.contains(LIVE_CHAT_REPLAY_MARKER)) return ActionButton.LIVE_CHAT_REPLAY;
        if (contents.contains(DETAILS_MARKER)) return ActionButton.DETAILS;
        if (contents.contains(LYRICS_MARKER)) return ActionButton.LYRICS;
        if (contents.contains(SHARE_MARKER)) return ActionButton.SHARE;
        if (contents.contains(RADIO_MARKER)) return ActionButton.RADIO;

        // Save markers are icon names, which are present in the shared icon catalogue.
        // Check only near the head of the buffer to avoid matching the shared catalogue.
        String head = contents.length() > ICON_SCAN_HEAD_LIMIT
                ? contents.substring(0, ICON_SCAN_HEAD_LIMIT) : contents;
        if (head.contains(SAVE_MARKER) || head.contains(SAVE_MARKER_ALT)) {
            return ActionButton.SAVE;
        }
        // Disabled buttons have no endpoint id, their own icon appears near the buffer head,
        // before the shared icon catalogue appended later in the same proto.
        if (head.contains(THUMB_UP_ICON) || head.contains(THUMB_DOWN_ICON)) {
            return ActionButton.LIKE_DISLIKE;
        }
        if (head.contains(COMMENTS_ICON)) return ActionButton.COMMENTS;
        if (head.contains(LYRICS_ICON)) return ActionButton.LYRICS;
        return null;
    }

    @Override
    public boolean isFiltered(Object contextSource, String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        return isFiltered(path, identifier, accessibility, buffer, matchedGroup, contentType, contentIndex);
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (!path.contains(VIDEO_ACTION_BAR_PATH_PREFIX)) {
            return false;
        }
        if (matchedGroup == actionBarRule && !isEveryFilterGroupEnabled()) {
            return false;
        }
        if (matchedGroup == genericActionButtonRule) {
            if (!(path.contains("|button.eml-fe|") || path.contains("|button.eml|")) || path.contains("button_inner")) {
                return false;
            }
            String contents = new String(buffer, StandardCharsets.ISO_8859_1);
            if (contents.contains(LIVE_CHAT_REPLAY_MARKER)) {
                return Settings.HIDE_ACTION_BUTTON_LIVE_CHAT_REPLAY.get();
            } else if (contents.contains(DETAILS_MARKER)) {
                return Settings.HIDE_ACTION_BUTTON_DETAILS.get();
            } else if (contents.contains(COMMENTS_MARKER)) {
                return Settings.HIDE_ACTION_BUTTON_COMMENT.get();
            } else if (contents.contains(LYRICS_MARKER)) {
                return Settings.HIDE_ACTION_BUTTON_LYRICS.get();
            } else if (contents.contains(SHARE_MARKER)) {
                return Settings.HIDE_ACTION_BUTTON_SHARE.get();
            } else if (contents.contains(RADIO_MARKER)) {
                return Settings.HIDE_ACTION_BUTTON_RADIO.get();
            } else {
                // Save markers are icon names, which are present in the shared icon catalogue.
                // Check only near the head of the buffer to avoid matching the shared catalogue.
                String head = contents.length() > ICON_SCAN_HEAD_LIMIT
                        ? contents.substring(0, ICON_SCAN_HEAD_LIMIT) : contents;
                if (head.contains(SAVE_MARKER) || head.contains(SAVE_MARKER_ALT)) {
                    return Settings.HIDE_ACTION_BUTTON_ADD_TO_PLAYLIST.get();
                } else {
                    return false;
                }
            }
        }
        if (matchedGroup == bufferFilterPathRule) {
            return !path.contains(VIDEO_ACTION_BUTTON_WRAPPER_PREFIX) && matchesButtonContent(buffer);
        }
        return true;
    }
}
