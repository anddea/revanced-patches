package app.morphe.patches.reddit.utils.extension.hooks

import app.morphe.patcher.Fingerprint
import app.morphe.patches.shared.extension.extensionHook

internal val applicationInitHook = extensionHook(
    fingerprint = Fingerprint(
        custom = { method, _ ->
            method.definingClass.endsWith("/FrontpageApplication;") &&
                    method.name == "onCreate"
        }
    )
)

