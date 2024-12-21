package app.revanced.patches.reddit.utils.compatibility

import app.revanced.patcher.patch.PackageName
import app.revanced.patcher.patch.VersionName

internal object Constants {
    internal const val REDDIT_PACKAGE_NAME = "com.reddit.frontpage"

    val COMPATIBLE_PACKAGE: Pair<PackageName, Set<VersionName>?> = Pair(
        REDDIT_PACKAGE_NAME,
        null
    )
}