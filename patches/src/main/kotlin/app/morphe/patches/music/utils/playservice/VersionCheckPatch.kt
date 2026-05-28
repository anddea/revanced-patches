package app.morphe.patches.music.utils.playservice

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import kotlin.properties.Delegates

var is_6_27_or_greater : Boolean by Delegates.notNull()
    private set
var is_6_36_or_greater : Boolean by Delegates.notNull()
    private set
var is_6_39_or_greater : Boolean by Delegates.notNull()
    private set
var is_6_42_or_greater : Boolean by Delegates.notNull()
    private set
var is_6_43_or_greater : Boolean by Delegates.notNull()
    private set
var is_6_48_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_03_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_06_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_13_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_16_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_17_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_18_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_20_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_23_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_25_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_27_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_28_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_29_or_greater : Boolean by Delegates.notNull()
    private set
var is_7_33_or_greater : Boolean by Delegates.notNull()
    private set
var is_8_03_or_greater : Boolean by Delegates.notNull()
    private set
var is_8_05_or_greater : Boolean by Delegates.notNull()
    private set
var is_8_07_or_greater : Boolean by Delegates.notNull()
    private set
var is_8_12_or_greater = false
    private set
var is_8_15_or_greater : Boolean by Delegates.notNull()
    private set
var is_8_28_or_greater : Boolean by Delegates.notNull()
    private set
var is_8_29_or_greater : Boolean by Delegates.notNull()
    private set
var is_8_30_or_greater : Boolean by Delegates.notNull()
    private set
var is_8_33_or_greater : Boolean by Delegates.notNull()
    private set

val versionCheckPatch = resourcePatch(
    description = "versionCheckPatch",
) {
    execute {
        val versionName = packageMetadata.versionName
        val isYouTubeMusic = packageMetadata.packageName == YOUTUBE_MUSIC_PACKAGE_NAME
        fun isEqualsOrGreaterThan(version: String): Boolean {
            return isYouTubeMusic && versionName >= version
        }

        // All bug fix releases always seem to use the same play store version as the minor version.
        is_6_27_or_greater = isEqualsOrGreaterThan("6.27.00")
        is_6_36_or_greater = isEqualsOrGreaterThan("6.36.00")
        is_6_39_or_greater = isEqualsOrGreaterThan("6.39.00")
        is_6_42_or_greater = isEqualsOrGreaterThan("6.42.00")
        is_6_43_or_greater = isEqualsOrGreaterThan("6.43.00")
        is_6_48_or_greater = isEqualsOrGreaterThan("6.48.00")
        is_7_03_or_greater = isEqualsOrGreaterThan("7.03.00")
        is_7_06_or_greater = isEqualsOrGreaterThan("7.06.00")
        is_7_13_or_greater = isEqualsOrGreaterThan("7.13.00")
        is_7_16_or_greater = isEqualsOrGreaterThan("7.16.00")
        is_7_17_or_greater = isEqualsOrGreaterThan("7.17.00")
        is_7_18_or_greater = isEqualsOrGreaterThan("7.18.00")
        is_7_20_or_greater = isEqualsOrGreaterThan("7.20.00")
        is_7_23_or_greater = isEqualsOrGreaterThan("7.23.00")
        is_7_25_or_greater = isEqualsOrGreaterThan("7.25.00")
        is_7_27_or_greater = isEqualsOrGreaterThan("7.27.00")
        is_7_28_or_greater = isEqualsOrGreaterThan("7.28.00")
        is_7_29_or_greater = isEqualsOrGreaterThan("7.29.00")
        is_7_33_or_greater = isEqualsOrGreaterThan("7.33.00")
        is_8_03_or_greater = isEqualsOrGreaterThan("8.03.00")
        is_8_05_or_greater = isEqualsOrGreaterThan("8.05.00")
        is_8_07_or_greater = isEqualsOrGreaterThan("8.07.00")
        is_8_12_or_greater = isEqualsOrGreaterThan("8.12.00")
        is_8_15_or_greater = isEqualsOrGreaterThan("8.15.00")
        is_8_28_or_greater = isEqualsOrGreaterThan("8.28.00")
        is_8_29_or_greater = isEqualsOrGreaterThan("8.29.00")
        is_8_30_or_greater = isEqualsOrGreaterThan("8.30.00")
        is_8_33_or_greater = isEqualsOrGreaterThan("8.33.00")
    }
}
