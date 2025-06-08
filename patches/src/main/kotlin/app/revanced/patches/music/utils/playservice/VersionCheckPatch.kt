@file:Suppress("ktlint:standard:property-naming")

package app.revanced.patches.music.utils.playservice

import app.revanced.patcher.patch.resourcePatch
import app.revanced.util.findElementByAttributeValueOrThrow

var is_6_27_or_greater = false
    private set
var is_6_36_or_greater = false
    private set
var is_6_39_or_greater = false
    private set
var is_6_42_or_greater = false
    private set
var is_6_43_or_greater = false
    private set
var is_6_48_or_greater = false
    private set
var is_7_03_or_greater = false
    private set
var is_7_06_or_greater = false
    private set
var is_7_13_or_greater = false
    private set
var is_7_17_or_greater = false
    private set
var is_7_18_or_greater = false
    private set
var is_7_20_or_greater = false
    private set
var is_7_23_or_greater = false
    private set
var is_7_25_or_greater = false
    private set
var is_7_27_or_greater = false
    private set
var is_7_28_or_greater = false
    private set
var is_7_29_or_greater = false
    private set
var is_7_33_or_greater = false
    private set
var is_8_03_or_greater = false
    private set
var is_8_05_or_greater = false
    private set
var is_8_15_or_greater = false
    private set
var is_8_16_or_greater = false
    private set
var is_8_19_or_greater = false
    private set

val versionCheckPatch = resourcePatch(
    description = "versionCheckPatch",
) {
    execute {
        // The app version is missing from the decompiled manifest,
        // so instead use the Google Play services version and compare against specific releases.
        val playStoreServicesVersion = document("res/values/integers.xml").use { document ->
            document.documentElement.childNodes.findElementByAttributeValueOrThrow(
                "name",
                "google_play_services_version",
            ).textContent.toInt()
        }

        // All bug fix releases always seem to use the same play store version as the minor version.
        is_6_27_or_greater = 234412000 <= playStoreServicesVersion
        is_6_36_or_greater = 240399000 <= playStoreServicesVersion
        is_6_39_or_greater = 240699000 <= playStoreServicesVersion
        is_6_42_or_greater = 240999000 <= playStoreServicesVersion
        is_6_43_or_greater = 241099000 <= playStoreServicesVersion
        is_6_48_or_greater = 241599000 <= playStoreServicesVersion
        is_7_03_or_greater = 242199000 <= playStoreServicesVersion
        is_7_06_or_greater = 242499000 <= playStoreServicesVersion
        is_7_13_or_greater = 243199000 <= playStoreServicesVersion
        is_7_17_or_greater = 243530000 <= playStoreServicesVersion
        is_7_18_or_greater = 243699000 <= playStoreServicesVersion
        is_7_20_or_greater = 243899000 <= playStoreServicesVersion
        is_7_23_or_greater = 244199000 <= playStoreServicesVersion
        is_7_25_or_greater = 244399000 <= playStoreServicesVersion
        is_7_27_or_greater = 244515000 <= playStoreServicesVersion
        is_7_28_or_greater = 244699000 <= playStoreServicesVersion
        is_7_29_or_greater = 244799000 <= playStoreServicesVersion
        is_7_33_or_greater = 245199000 <= playStoreServicesVersion
        is_8_03_or_greater = 250399000 <= playStoreServicesVersion
        is_8_05_or_greater = 250599000 <= playStoreServicesVersion
        is_8_15_or_greater = 251530000 <= playStoreServicesVersion
        is_8_16_or_greater = 251630000 <= playStoreServicesVersion
        is_8_19_or_greater = 251930000 <= playStoreServicesVersion
    }
}
