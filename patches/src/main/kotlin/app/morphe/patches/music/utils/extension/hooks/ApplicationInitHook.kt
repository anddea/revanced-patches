package app.morphe.patches.music.utils.extension.hooks

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patches.shared.extension.extensionHook

internal val applicationInitHook = extensionHook(
    fingerprint = Fingerprint(
        name = "onCreate",
        returnType = "V",
        parameters = listOf(),
        filters = listOf(
            string("activity")
        )
    )
)
