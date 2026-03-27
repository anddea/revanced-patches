package app.morphe.patches.youtube.utils.extension.hooks

import app.morphe.patcher.Fingerprint
import app.morphe.patches.shared.extension.extensionHook

/**
 * Hooks the context when the app is launched as a regular application (and is not an embedded video playback).
 */
// Extension context is the Activity itself.
internal val applicationInitHook = extensionHook(
    fingerprint = Fingerprint(
        strings = listOf("Application creation", "Application.onCreate"),
    )
)
