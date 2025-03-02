package app.revanced.patches.reddit.utils.compatibility

import app.revanced.patcher.patch.PackageName
import app.revanced.patcher.patch.VersionName

internal object Constants {
    internal const val REDDIT_PACKAGE_NAME = "com.reddit.frontpage"

    val COMPATIBLE_PACKAGE: Pair<PackageName, Set<VersionName>?> = Pair(
        REDDIT_PACKAGE_NAME,
        setOf(
            "2024.17.0", // This is the last version that can be patched without anti-split.
            "2025.05.1", // This is the latest version supported by the RVX patch.
        )
    )
}