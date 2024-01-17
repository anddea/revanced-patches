package app.revanced.patches.music.misc.bitrate

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch

@Patch(
    name = "Bitrate default value",
    description = "Sets the audio quality to \"Always High\" when you first install the app.",
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object BitrateDefaultValuePatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {
        context.xmlEditor[RESOURCE_FILE_PATH].use { editor ->
            editor.file.getElementsByTagName("com.google.android.apps.youtube.music.ui.preference.PreferenceCategoryCompat")
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

    }

    private const val RESOURCE_FILE_PATH = "res/xml/data_saving_settings.xml"
}