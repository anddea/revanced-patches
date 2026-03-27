@file:Suppress("ktlint:standard:property-naming")

package app.morphe.patches.youtube.utils.playservice

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.findElementByAttributeValueOrThrow

var is_18_31_or_greater = false
    private set
var is_18_34_or_greater = false
    private set
var is_18_39_or_greater = false
    private set
var is_18_42_or_greater = false
    private set
var is_18_49_or_greater = false
    private set
var is_19_01_or_greater = false
    private set
var is_19_02_or_greater = false
    private set
var is_19_04_or_greater = false
    private set
var is_19_05_or_greater = false
    private set
var is_19_09_or_greater = false
    private set
var is_19_11_or_greater = false
    private set
var is_19_15_or_greater = false
    private set
var is_19_16_or_greater = false
    private set
var is_19_17_or_greater = false
    private set
var is_19_18_or_greater = false
    private set
var is_19_23_or_greater = false
    private set
var is_19_25_or_greater = false
    private set
var is_19_26_or_greater = false
    private set
var is_19_28_or_greater = false
    private set
var is_19_29_or_greater = false
    private set
var is_19_30_or_greater = false
    private set
var is_19_32_or_greater = false
    private set
var is_19_34_or_greater = false
    private set
var is_19_36_or_greater = false
    private set
var is_19_37_or_greater = false
    private set
var is_19_41_or_greater = false
    private set
var is_19_42_or_greater = false
    private set
var is_19_43_or_greater = false
    private set
var is_19_44_or_greater = false
    private set
var is_19_46_or_greater = false
    private set
var is_19_49_or_greater = false
    private set
var is_19_50_or_greater = false
    private set
var is_20_02_or_greater = false
    private set
var is_20_03_or_greater = false
    private set
var is_20_04_or_greater = false
    private set
var is_20_05_or_greater = false
    private set
var is_20_06_or_greater = false
    private set
var is_20_07_or_greater = false
    private set
var is_20_09_or_greater = false
    private set
var is_20_10_or_greater = false
    private set
var is_20_12_or_greater = false
    private set
var is_20_13_or_greater = false
    private set
var is_20_14_or_greater = false
    private set
var is_20_15_or_greater = false
    private set
var is_20_16_or_greater = false
    private set
var is_20_18_or_greater = false
    private set
var is_20_19_or_greater = false
    private set
var is_20_20_or_greater = false
    private set
var is_20_21_or_greater = false
    private set
var is_20_22_or_greater = false
    private set
var is_20_28_or_greater = false
    private set
var is_20_30_or_greater = false
    private set
var is_20_49_or_greater = false
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

        fun isGreaterThan(targetVersion: Int) =
            targetVersion <= playStoreServicesVersion

        // All bug fix releases always seem to use the same play store version as the minor version.
        is_18_31_or_greater = isGreaterThan(233200000)
        is_18_34_or_greater = isGreaterThan(233500000)
        is_18_39_or_greater = isGreaterThan(234000000)
        is_18_42_or_greater = isGreaterThan(234302000)
        is_18_49_or_greater = isGreaterThan(235000000)
        is_19_01_or_greater = isGreaterThan(240204000)
        is_19_02_or_greater = isGreaterThan(240299000)
        is_19_04_or_greater = isGreaterThan(240502000)
        is_19_05_or_greater = isGreaterThan(240602000)
        is_19_09_or_greater = isGreaterThan(241002000)
        is_19_11_or_greater = isGreaterThan(241199000)
        is_19_15_or_greater = isGreaterThan(241602000)
        is_19_16_or_greater = isGreaterThan(241702000)
        is_19_17_or_greater = isGreaterThan(241802000)
        is_19_18_or_greater = isGreaterThan(241902000)
        is_19_23_or_greater = isGreaterThan(242402000)
        is_19_25_or_greater = isGreaterThan(242599000)
        is_19_26_or_greater = isGreaterThan(242705000)
        is_19_28_or_greater = isGreaterThan(242905000)
        is_19_29_or_greater = isGreaterThan(243005000)
        is_19_30_or_greater = isGreaterThan(243105000)
        is_19_32_or_greater = isGreaterThan(243305000)
        is_19_34_or_greater = isGreaterThan(243499000)
        is_19_36_or_greater = isGreaterThan(243705000)
        is_19_37_or_greater = isGreaterThan(243805000)
        is_19_41_or_greater = isGreaterThan(244205000)
        is_19_42_or_greater = isGreaterThan(244305000)
        is_19_43_or_greater = isGreaterThan(244405000)
        is_19_44_or_greater = isGreaterThan(244505000)
        is_19_46_or_greater = isGreaterThan(244705000)
        is_19_49_or_greater = isGreaterThan(245005000)
        is_19_50_or_greater = isGreaterThan(245105000)
        is_20_02_or_greater = isGreaterThan(250299000)
        is_20_03_or_greater = isGreaterThan(250405000)
        is_20_04_or_greater = isGreaterThan(250505000)
        is_20_05_or_greater = isGreaterThan(250605000)
        is_20_06_or_greater = isGreaterThan(250705000)
        is_20_07_or_greater = isGreaterThan(250805000)
        is_20_09_or_greater = isGreaterThan(251006000)
        is_20_10_or_greater = isGreaterThan(251105000)
        is_20_12_or_greater = isGreaterThan(251305000)
        is_20_13_or_greater = isGreaterThan(251405000)
        is_20_14_or_greater = isGreaterThan(251505000)
        is_20_15_or_greater = isGreaterThan(251605000)
        is_20_16_or_greater = isGreaterThan(251705000)
        is_20_18_or_greater = isGreaterThan(251905000)
        is_20_19_or_greater = isGreaterThan(252005000)
        is_20_19_or_greater = isGreaterThan(252005000)
        is_20_20_or_greater = isGreaterThan(252105000)
        is_20_20_or_greater = isGreaterThan(252105000)
        is_20_21_or_greater = isGreaterThan(252205000)
        is_20_21_or_greater = isGreaterThan(252205000)
        is_20_22_or_greater = isGreaterThan(252305000)
        is_20_22_or_greater = isGreaterThan(252305000)
        is_20_28_or_greater = isGreaterThan(252905000)
        is_20_30_or_greater = isGreaterThan(253105000)
        is_20_49_or_greater = isGreaterThan(255005000)
    }
}
