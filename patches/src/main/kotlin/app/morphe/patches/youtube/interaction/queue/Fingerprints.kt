// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.morphe.patches.youtube.interaction.queue

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object ActiveDevicesRequestFingerprint : Fingerprint(
    filters = listOf(string("mdx/get_active_devices"))
)

/** Resolves a menu command, the ⋮ of a card or player, into the menu it shows; param 0 is the command. */
internal object MenuCommandResolverFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, _ -> method.parameterTypes.contains("Ljava/util/Map;") },
    strings = listOf(
        string("com.google.android.libraries.youtube.rendering.elements.sender_view"),
        string("com.google.android.libraries.youtube.innertube.endpoint.tag")
    ),
    filters = listOf(methodCall(definingClass = "Landroid/view/Window;", name = "getNavigationBarColor"))
)

// The queue-add resolver switches request paths with this experiment.
internal object QueueInsertResolverFingerprint : Fingerprint(
    parameters = listOf("L", "Ljava/util/Map;"),
    returnType = "V",
    filters = listOf(literal(45388674L))
)
