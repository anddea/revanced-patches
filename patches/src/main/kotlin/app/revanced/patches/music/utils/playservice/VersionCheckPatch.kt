@file:Suppress("ktlint:standard:property-naming")

package app.revanced.patches.music.utils.playservice

import app.revanced.patcher.patch.resourcePatch
import app.revanced.util.findElementByAttributeValueOrThrow

var is_6_27_or_greater = false
    private set
var is_6_36_or_greater = false
    private set
var is_6_42_or_greater = false
    private set
var is_7_06_or_greater = false
    private set
var is_7_13_or_greater = false
    private set
var is_7_18_or_greater = false
    private set
var is_7_20_or_greater = false
    private set
var is_7_23_or_greater = false
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
        is_6_42_or_greater = 240999000 <= playStoreServicesVersion
        is_7_06_or_greater = 242499000 <= playStoreServicesVersion
        is_7_13_or_greater = 243199000 <= playStoreServicesVersion
        is_7_18_or_greater = 243699000 <= playStoreServicesVersion
        is_7_20_or_greater = 243899000 <= playStoreServicesVersion
        is_7_23_or_greater = 244199000 <= playStoreServicesVersion
    }
}
