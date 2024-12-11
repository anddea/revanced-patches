package app.revanced.patches.reddit.utils.compatibility

import app.revanced.patcher.patch.PackageName
import app.revanced.patcher.patch.VersionName

internal object Constants {
    val COMPATIBLE_PACKAGE: Pair<PackageName, Set<VersionName>?> = Pair(
        "com.reddit.frontpage",
        setOf(
            "2023.12.0",
            "2024.17.0"
        )
    )
}