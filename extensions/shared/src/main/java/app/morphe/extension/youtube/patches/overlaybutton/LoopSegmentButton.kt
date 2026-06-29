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

package app.morphe.extension.youtube.patches.overlaybutton

import android.content.Intent
import android.net.Uri
import android.view.View
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.shared.utils.StringRef.str
import app.morphe.extension.shared.utils.Utils
import app.morphe.extension.shared.utils.Utils.showToastShort
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.shared.PlayerControlButton
import app.morphe.extension.youtube.shared.RootView.isAdProgressTextVisible
import app.morphe.extension.youtube.shared.VideoInformation
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.max
import androidx.core.net.toUri

@Suppress("unused")
object LoopSegmentButton {
    private const val UNSET = -1L
    private const val INTENT_DISMISS_GRACE_PERIOD_MS = 1500L
    private const val SEEK_COOLDOWN_MS = 500L
    private const val END_SCREEN_BUFFER_MS = 1500L
    private val relativeTimePattern = Pattern.compile("(\\d+)([hms])")
    private val urlPattern = Pattern.compile("(https?://\\S+|vnd\\.youtube:\\S+)")

    private var instance: PlayerControlButton? = null
    private var segmentStartMs = UNSET
    private var segmentEndMs = UNSET
    private var segmentVideoId = ""
    private var segmentEndIsVideoEnd = false
    private var lastSeekTimeMs = 0L
    private var lastSegmentIntentTimeMs = 0L
    private var metadataVideoId = ""
    private var metadataVideoLengthMs = 0L
    private var pendingSegment: LoopSegment? = null

    /**
     * Injection point.
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_loop_segment_button",
                buttonVisibility = { isButtonEnabled() },
                onClickListener = { onClick() },
                onLongClickListener = {
                    onLongClick()
                    true
                }
            )
            updateButtonState()
        } catch (ex: Exception) {
            Logger.printException({ "initializeButton failure" }, ex)
        }
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setVisibilityNegatedImmediate() {
        instance?.setVisibilityNegatedImmediate()
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setVisibilityImmediate(visible: Boolean) {
        if (visible) updateButtonState()
        instance?.setVisibilityImmediate(visible)
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setVisibility(visible: Boolean, animated: Boolean) {
        if (visible) updateButtonState()
        instance?.setVisibility(visible, animated)
    }

    /**
     * Injection point. Called with incoming YouTube intents.
     *
     * URL segments are handled independently of the overlay button setting so shared links loop
     * even when the button is disabled.
     */
    @JvmStatic
    fun handleIntent(intent: Intent?) {
        try {
            pendingSegment = parseIntentSegment(intent) ?: return
            lastSegmentIntentTimeMs = System.currentTimeMillis()
            applyPendingSegment(VideoInformation.getVideoId())
        } catch (ex: Exception) {
            Logger.printException({ "handleIntent failure" }, ex)
        }
    }

    /**
     * Injection point. Called when video metadata is loaded.
     */
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun newVideoStarted(
        newlyLoadedChannelId: String,
        newlyLoadedChannelName: String,
        newlyLoadedVideoId: String,
        newlyLoadedVideoTitle: String,
        newlyLoadedVideoLength: Long,
        newlyLoadedLiveStreamValue: Boolean,
    ) {
        try {
            updateVideoMetadata(newlyLoadedVideoId, newlyLoadedVideoLength)

            if (applyPendingSegment(newlyLoadedVideoId, newlyLoadedVideoLength)) {
                return
            }

            if (segmentVideoId.isNotEmpty() && segmentVideoId != newlyLoadedVideoId) {
                clearSegment(showToast = false)
            }
        } catch (ex: Exception) {
            Logger.printException({ "newVideoStarted failure" }, ex)
        }
    }

    /**
     * Injection point. Called when the player is closed.
     */
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun onPlayerDismissed(dismissType: Int) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastSegmentIntentTimeMs < INTENT_DISMISS_GRACE_PERIOD_MS) return

            clearAllState()
        } catch (ex: Exception) {
            Logger.printException({ "onPlayerDismissed failure" }, ex)
        }
    }

    /**
     * Injection point. Called with current video time in milliseconds.
     */
    @JvmStatic
    fun videoTimeChanged(time: Long) {
        try {
            val currentVideoId = VideoInformation.getVideoId()
            if (currentVideoId.isEmpty()) return

            applyPendingSegment(currentVideoId)

            if (segmentVideoId.isNotEmpty() && segmentVideoId != currentVideoId) {
                clearSegment(showToast = false)
                return
            }

            if (!isSegmentActive()) return

            val triggerMs = if (segmentEndIsVideoEnd) {
                max(segmentStartMs + 1, segmentEndMs - END_SCREEN_BUFFER_MS)
            } else {
                segmentEndMs
            }
            if (time < triggerMs) return

            val now = System.currentTimeMillis()
            if (now - lastSeekTimeMs < SEEK_COOLDOWN_MS) return
            lastSeekTimeMs = now

            seekToSegmentStart()
        } catch (ex: Exception) {
            Logger.printException({ "videoTimeChanged failure" }, ex)
        }
    }

    /**
     * Injection point. Fallback for when playback reaches the end before the time hook loops.
     */
    @JvmStatic
    fun videoEnded(): Boolean {
        try {
            val currentVideoId = VideoInformation.getVideoId()
            if (segmentVideoId.isNotEmpty() && currentVideoId.isNotEmpty() && segmentVideoId != currentVideoId) {
                clearSegment(showToast = false)
                return false
            }

            if (!isSegmentActive()) return false

            val now = System.currentTimeMillis()
            if (now - lastSeekTimeMs < SEEK_COOLDOWN_MS) return true
            lastSeekTimeMs = now

            return seekToSegmentStart()
        } catch (ex: Exception) {
            Logger.printException({ "videoEnded failure" }, ex)
            return false
        }
    }

    private fun isButtonEnabled(): Boolean {
        return Settings.OVERLAY_BUTTON_LOOP_SEGMENT.get() && !isAdProgressTextVisible()
    }

    private fun onClick() {
        val currentVideoId = VideoInformation.getVideoId()
        val currentTimeMs = VideoInformation.getVideoTime()
        if (currentVideoId.isEmpty() || currentTimeMs < 0 || VideoInformation.getLiveStreamState()) {
            showToastShort(str("revanced_overlay_button_loop_segment_invalid_time_toast"))
            return
        }

        pendingSegment = null

        if (segmentVideoId.isNotEmpty() && segmentVideoId != currentVideoId) {
            clearSegment(showToast = false)
        }

        when {
            isSegmentActive() -> clearSegment(showToast = true)
            segmentStartMs < 0 -> selectStart(currentVideoId, currentTimeMs)
            else -> selectEnd(currentVideoId, currentTimeMs)
        }
    }

    private fun onLongClick() {
        if (!isSegmentActive()) {
            showToastShort(str("revanced_overlay_button_loop_segment_copy_unavailable_toast"))
            return
        }

        val url = buildSegmentUrl(segmentVideoId, segmentStartMs, segmentEndMs)
        Utils.setClipboard(url, str("revanced_overlay_button_loop_segment_copy_toast"))
    }

    private fun selectStart(videoId: String, startMs: Long) {
        segmentStartMs = startMs
        segmentEndMs = UNSET
        segmentVideoId = videoId
        segmentEndIsVideoEnd = false
        lastSeekTimeMs = 0
        updateButtonState()
        showToastShort(str("revanced_overlay_button_loop_segment_start_toast", formatTime(startMs)))
    }

    private fun selectEnd(videoId: String, endMs: Long) {
        if (videoId != segmentVideoId || endMs <= segmentStartMs) {
            showToastShort(str("revanced_overlay_button_loop_segment_invalid_time_toast"))
            return
        }

        val videoLength = getVideoLength(videoId)
        segmentEndIsVideoEnd = videoLength > 0 && endMs >= videoLength - 500
        segmentEndMs = if (segmentEndIsVideoEnd) videoLength else endMs
        lastSeekTimeMs = 0
        updateButtonState()
        showToastShort(
            str(
                "revanced_overlay_button_loop_segment_enabled_toast",
                formatTime(segmentStartMs),
                formatTime(segmentEndMs)
            )
        )
    }

    private fun clearSegment(showToast: Boolean) {
        segmentStartMs = UNSET
        segmentEndMs = UNSET
        segmentVideoId = ""
        segmentEndIsVideoEnd = false
        lastSeekTimeMs = 0
        lastSegmentIntentTimeMs = 0
        updateButtonState()
        if (showToast) {
            showToastShort(str("revanced_overlay_button_loop_segment_cleared_toast"))
        }
    }

    private fun applyPendingSegment(currentVideoId: String) {
        applyPendingSegment(currentVideoId, getVideoLength(currentVideoId))
    }

    private fun applyPendingSegment(currentVideoId: String, videoLength: Long): Boolean {
        val pending = pendingSegment ?: return false
        if (currentVideoId != pending.videoId) return false

        val endMs = if (videoLength > 0 && pending.endMs >= videoLength - 500) videoLength else pending.endMs
        if (endMs <= pending.startMs) {
            pendingSegment = null
            clearSegment(showToast = false)
            return true
        }

        pendingSegment = null
        segmentStartMs = pending.startMs
        segmentEndMs = endMs
        segmentVideoId = pending.videoId
        segmentEndIsVideoEnd = endMs == videoLength
        lastSeekTimeMs = 0
        updateButtonState()
        return true
    }

    private fun isSegmentActive() = segmentStartMs >= 0 && segmentEndMs > segmentStartMs

    private fun seekToSegmentStart(): Boolean {
        if (segmentStartMs < 0) return false

        val videoLength = getVideoLength(segmentVideoId)
        if (videoLength <= 0) return false

        return VideoInformation.seekTo(segmentStartMs, videoLength)
    }

    private fun updateVideoMetadata(videoId: String, videoLength: Long) {
        if (videoId.isEmpty()) return
        if (metadataVideoId != videoId) {
            metadataVideoId = videoId
            metadataVideoLengthMs = 0
        }
        if (videoLength > 0) {
            metadataVideoLengthMs = videoLength
        }
    }

    private fun getVideoLength(videoId: String): Long {
        val videoLength = VideoInformation.getVideoLength()
        val currentVideoId = VideoInformation.getVideoId()
        if (videoLength > 0 && videoId.isNotEmpty() && (currentVideoId.isEmpty() || currentVideoId == videoId)) {
            updateVideoMetadata(videoId, videoLength)
            return videoLength
        }

        return if (videoId.isNotEmpty() && videoId == metadataVideoId) metadataVideoLengthMs else 0
    }

    private fun clearAllState() {
        pendingSegment = null
        clearSegment(showToast = false)
    }

    private fun updateButtonState() {
        Utils.runOnMainThread {
            instance?.imageView()?.apply {
                isSelected = segmentStartMs >= 0
                isActivated = isSegmentActive()
            }
        }
    }

    private fun buildSegmentUrl(videoId: String, startMs: Long, endMs: Long): String {
        return "https://youtu.be/$videoId?start=${startMs / 1000}&end=${endMs / 1000}"
    }

    private fun parseIntentSegment(intent: Intent?): LoopSegment? {
        if (intent == null) return null

        parseUriSegment(intent.data)?.let { return it }
        parseUriSegment(getUriExtra(intent, "playlist_uri"))?.let { return it }
        parseUriSegment(parseUriString(intent.getStringExtra(Intent.EXTRA_TEXT)))?.let { return it }
        parseUriSegment(parseUriString(intent.getStringExtra("url")))?.let { return it }

        return null
    }

    private fun parseUriSegment(uri: Uri?): LoopSegment? {
        if (uri == null) return null

        val endSeconds = parseTimeSeconds(uri.getQueryParameter("end")) ?: return null
        val startSeconds = parseTimeSeconds(
            uri.getQueryParameter("start") ?: uri.getQueryParameter("t")
        ) ?: 0
        val videoId = parseVideoId(uri) ?: return null
        if (endSeconds <= startSeconds) return null

        return LoopSegment(videoId, startSeconds * 1000, endSeconds * 1000)
    }

    @Suppress("DEPRECATION", "SameParameterValue")
    private fun getUriExtra(intent: Intent, key: String): Uri? {
        return try {
            intent.getParcelableExtra(key) as? Uri
        } catch (_: Exception) {
            null
        }
    }

    private fun parseUriString(value: String?): Uri? {
        val input = value?.trim() ?: return null
        if (input.isEmpty()) return null

        val matcher = urlPattern.matcher(input)
        val uriString = (if (matcher.find()) matcher.group(1) else input) ?: return null
        return uriString.trimEnd(')', ']', '>', '.', ',').toUri()
    }

    private fun parseVideoId(uri: Uri): String? {
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        if (uri.scheme == "vnd.youtube" && host.isNotEmpty()) {
            return host
        }

        if (host == "youtu.be") {
            return uri.pathSegments.firstOrNull()?.takeIf { it.isNotEmpty() }
        }

        if (!host.endsWith("youtube.com") && !host.endsWith("youtube-nocookie.com")) {
            return null
        }

        uri.getQueryParameter("v")?.takeIf { it.isNotEmpty() }?.let { return it }

        val segments = uri.pathSegments
        if (segments.size >= 2 && segments[0] in setOf("shorts", "embed", "v")) {
            return segments[1].takeIf { it.isNotEmpty() }
        }

        return null
    }

    private fun parseTimeSeconds(value: String?): Long? {
        val input = value?.trim()?.lowercase(Locale.ROOT) ?: return null
        if (input.isEmpty()) return null

        input.toLongOrNull()?.let { return it }

        if (input.contains(":")) {
            val parts = input.split(":")
            if (parts.isEmpty() || parts.size > 3) return null
            var totalSeconds = 0L
            for (part in parts) {
                val parsed = part.toLongOrNull() ?: return null
                totalSeconds = totalSeconds * 60 + parsed
            }
            return totalSeconds
        }

        val matcher = relativeTimePattern.matcher(input)
        var totalSeconds = 0L
        var matched = false
        while (matcher.find()) {
            val amount = matcher.group(1)?.toLongOrNull() ?: continue
            val unit = matcher.group(2) ?: continue
            matched = true
            totalSeconds += when (unit) {
                "h" -> amount * 3600
                "m" -> amount * 60
                else -> amount
            }
        }

        return if (matched) totalSeconds else null
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }

    private data class LoopSegment(
        val videoId: String,
        val startMs: Long,
        val endMs: Long,
    )
}
