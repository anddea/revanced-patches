package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.revanced.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.revanced.patches.youtube.utils.patch.PatchList
import app.revanced.util.doRecursively
import app.revanced.util.insertNode
import org.w3c.dom.Element
import java.io.File

internal object ResourceUtils {
    private lateinit var context: ResourcePatchContext
    private lateinit var youtubeSettingFile: File
    private lateinit var rvxSettingFile: File

    fun setContext(context: ResourcePatchContext) {
        this.context = context
        this.youtubeSettingFile = context[YOUTUBE_SETTINGS_PATH]
        this.rvxSettingFile = context[RVX_PREFERENCE_PATH]
    }

    fun getContext() = context

    const val RVX_PREFERENCE_PATH = "res/xml/revanced_prefs.xml"
    const val YOUTUBE_SETTINGS_PATH = "res/xml/settings_fragment.xml"

    var youtubeMusicPackageName = YOUTUBE_MUSIC_PACKAGE_NAME
    var youtubePackageName = YOUTUBE_PACKAGE_NAME

    private var iconType = "default"
    fun getIconType() = iconType

    fun updatePackageName(
        fromPackageName: String,
        toPackageName: String,
        musicPackageName: String
    ) {
        youtubeMusicPackageName = musicPackageName
        youtubePackageName = toPackageName

        youtubeSettingFile.writeText(
            youtubeSettingFile.readText()
                .replace(
                    "android:targetPackage=\"$fromPackageName",
                    "android:targetPackage=\"$toPackageName"
                )
        )
    }

    fun updateGmsCorePackageName(
        fromPackageName: String,
        toPackageName: String
    ) {
        rvxSettingFile.writeText(
            rvxSettingFile.readText()
                .replace(
                    "android:targetPackage=\"$fromPackageName",
                    "android:targetPackage=\"$toPackageName"
                )
        )
    }

    fun addPreference(patch: PatchList) {
        patch.included = true
        updatePatchStatus(patch.title.replace(" for YouTube", ""))
    }

    fun addPreference(settingArray: Array<String>, patch: PatchList) {
        settingArray.forEach preferenceLoop@{ preference ->
            rvxSettingFile.writeText(
                rvxSettingFile.readText()
                    .replace("<!-- $preference", "")
                    .replace("$preference -->", "")
            )
        }

        addPreference(patch)
    }

    fun updatePatchStatus(patchTitle: String) {
        updatePatchStatusSettings(patchTitle, "@string/revanced_patches_included")
    }

    fun updatePatchStatusIcon(iconName: String) {
        iconType = iconName
        updatePatchStatusSettings("Icon", "@string/revanced_icon_$iconName")
    }

    fun updatePatchStatusLabel(appName: String) =
        updatePatchStatusSettings("Label", appName)

    fun updatePatchStatusTheme(themeName: String) =
        updatePatchStatusSettings("Theme", themeName)

    fun updatePatchStatusSettings(
        patchTitle: String,
        updateText: String
    ) = context.apply {
        document(RVX_PREFERENCE_PATH).use { document ->
            document.doRecursively loop@{
                if (it !is Element) return@loop

                it.getAttributeNode("android:title")?.let { attribute ->
                    if (attribute.textContent == patchTitle) {
                        it.getAttributeNode("android:summary").textContent = updateText
                    }
                }
            }
        }
    }

    fun addPreferenceFragment(key: String, insertKey: String) = context.apply {
        val targetClass =
            "com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity"

        document(YOUTUBE_SETTINGS_PATH).use { document ->
            with(document) {
                val processedKeys = mutableSetOf<String>() // To track processed keys

                doRecursively loop@{ node ->
                    if (node !is Element) return@loop // Skip if not an element

                    val attributeNode = node.getAttributeNode("android:key")
                        ?: return@loop // Skip if no key attribute
                    val currentKey = attributeNode.textContent

                    // Check if the current key has already been processed
                    if (processedKeys.contains(currentKey)) {
                        return@loop // Skip if already processed
                    } else {
                        processedKeys.add(currentKey) // Add the current key to processedKeys
                    }

                    when (currentKey) {
                        insertKey -> {
                            node.insertNode("Preference", node) {
                                setAttribute("android:key", "${key}_key")
                                setAttribute("android:title", "@string/${key}_title")
                                this.appendChild(
                                    ownerDocument.createElement("intent").also { intentNode ->
                                        intentNode.setAttribute(
                                            "android:targetPackage",
                                            youtubePackageName
                                        )
                                        intentNode.setAttribute("android:data", key + "_intent")
                                        intentNode.setAttribute("android:targetClass", targetClass)
                                    }
                                )
                            }
                            node.setAttribute("app:iconSpaceReserved", "true")
                        }

                        "true" -> {
                            attributeNode.textContent = "false"
                        }
                    }
                }
            }
        }
    }
}