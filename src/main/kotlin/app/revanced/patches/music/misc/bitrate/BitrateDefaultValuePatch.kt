package app.revanced.patches.music.misc.bitrate

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.patch.BaseResourcePatch

@Suppress("DEPRECATION", "unused")
object BitrateDefaultValuePatch : BaseResourcePatch(
    name = "Bitrate default value",
    description = "Sets the audio quality to 'Always High' when you first install the app.",
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val RESOURCE_FILE_PATH = "res/xml/data_saving_settings.xml"

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
}