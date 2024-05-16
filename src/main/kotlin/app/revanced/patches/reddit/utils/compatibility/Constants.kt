package app.revanced.patches.reddit.utils.compatibility

import app.revanced.patcher.patch.Patch

object Constants {
    val COMPATIBLE_PACKAGE =
        setOf(
            Patch.CompatiblePackage(
                "com.reddit.frontpage",
                setOf(
                    "2023.12.0",
                    "2024.17.0"
                )
            )
        )
}