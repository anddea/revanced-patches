/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
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

package app.morphe.extension.youtube.utils;

import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Keeps grounding metadata with cached answers without sending it back as conversation text.
 * The encoded trailer survives the existing string callback/cache pipeline; only API metadata
 * is rendered as Search HTML, never arbitrary HTML in the model's answer.
 */
final class GeminiGrounding {
    private static final String MARKER = "\n<!-- gemini-grounding:";
    static final String ANNOTATION = "gemini-search";

    private GeminiGrounding() {
    }

    static String attach(String text, JSONObject metadata) {
        if (metadata.length() == 0) return text;
        return text + MARKER + Base64.encodeToString(metadata.toString().getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP) + " -->";
    }

    static String strip(String text) {
        int index = text.lastIndexOf(MARKER);
        return index < 0 ? text : text.substring(0, index);
    }

    static JSONObject metadata(String text) {
        int index = text.lastIndexOf(MARKER);
        if (index >= 0 && text.endsWith(" -->")) {
            try {
                return new JSONObject(new String(Base64.decode(text.substring(index + MARKER.length(),
                        text.length() - 4), Base64.NO_WRAP), StandardCharsets.UTF_8));
            } catch (JSONException | IllegalArgumentException ignored) {
                // A malformed cached trailer must not prevent the answer from being displayed.
            }
        }
        return new JSONObject();
    }

    /** API segment indices are UTF-8 byte offsets, so convert before inserting citation labels. */
    static String cite(String text, JSONObject metadata) {
        JSONArray chunks = metadata.optJSONArray("groundingChunks");
        JSONArray supports = metadata.optJSONArray("groundingSupports");
        if (chunks == null || supports == null) return text;
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        java.util.TreeMap<Integer, StringBuilder> citations = new java.util.TreeMap<>();
        for (int i = 0; i < supports.length(); i++) {
            JSONObject support = supports.optJSONObject(i);
            if (support == null) continue;
            JSONObject segment = support.optJSONObject("segment");
            JSONArray indices = support.optJSONArray("groundingChunkIndices");
            int end = segment == null ? -1 : segment.optInt("endIndex", -1);
            if (end < 0 || end > bytes.length || indices == null) continue;
            int offset = new String(bytes, 0, end, StandardCharsets.UTF_8).length();
            StringBuilder links = citations.computeIfAbsent(offset, unused -> new StringBuilder());
            for (int j = 0; j < indices.length(); j++) {
                int index = indices.optInt(j, -1);
                JSONObject chunk = index >= 0 ? chunks.optJSONObject(index) : null;
                JSONObject web = chunk == null ? null : chunk.optJSONObject("web");
                String uri = web == null ? "" : web.optString("uri");
                if (uri.startsWith("https://") || uri.startsWith("http://")) {
                    links.append(" [").append(index + 1).append("](")
                            .append(uri.replace("(", "%28").replace(")", "%29")).append(")");
                }
            }
        }
        StringBuilder result = new StringBuilder(text);
        for (var entry : citations.descendingMap().entrySet()) result.insert(entry.getKey(), entry.getValue());
        return result.toString();
    }

    /** Preserve Google's supplied Search suggestion HTML/CSS and show an aggregate source list. */
    static String html(JSONObject metadata) {
        StringBuilder html = new StringBuilder();
        JSONArray chunks = metadata.optJSONArray("groundingChunks");
        if (chunks != null) {
            html.append("<ol>");
            for (int i = 0; i < chunks.length(); i++) {
                JSONObject chunk = chunks.optJSONObject(i);
                JSONObject web = chunk == null ? null : chunk.optJSONObject("web");
                if (web == null) continue;
                String uri = web.optString("uri");
                if (!uri.startsWith("https://") && !uri.startsWith("http://")) continue;
                html.append("<li value=\"").append(i + 1).append("\"><a href=\"")
                        .append(TextUtils.htmlEncode(uri)).append("\">")
                        .append(TextUtils.htmlEncode(web.optString("title", uri))).append("</a></li>");
            }
            html.append("</ol>");
        }
        JSONObject entry = metadata.optJSONObject("searchEntryPoint");
        if (entry != null) html.append(entry.optString("renderedContent"));
        return html.toString();
    }
}
