package app.revanced.patches.music.misc.bitrate

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.BITRATE_DEFAULT_VALUE
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.settingsPatch

@Suppress("unused")
val bitrateDefaultValuePatch = resourcePatch(
    BITRATE_DEFAULT_VALUE.title,
    BITRATE_DEFAULT_VALUE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        document("res/xml/data_saving_settings.xml").use { document ->
            document.getElementsByTagName("com.google.android.apps.youtube.music.ui.preference.PreferenceCategoryCompat")
                .item(0).childNodes.apply {
                    arrayOf("BitrateAudioMobile", "BitrateAudioWiFi").forEach {
                        for (i in 1 until length) {
                            val view = item(i)
                            if (
                                view.hasAttributes() &&
                                view.attributes.getNamedItem("android:key").nodeValue.endsWith(it)
                            ) {
                                view.attributes.getNamedItem("android:defaultValue").nodeValue =
                                    "Always High"
                                break
                            }
                        }
                    }
                }
        }

        updatePatchStatus(BITRATE_DEFAULT_VALUE)

    }
}