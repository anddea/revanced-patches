package app.morphe.patches.youtube.utils.settings

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.morphe.patches.youtube.utils.patch.PatchList
import app.morphe.util.doRecursively
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.insertNode
import org.w3c.dom.Element
import java.io.File

internal object ResourceUtils {
    internal const val RVX_PREFERENCE_PATH = "res/xml/revanced_prefs.xml"
    internal const val YOUTUBE_SETTINGS_PATH = "res/xml/settings_fragment.xml"

    private lateinit var context: ResourcePatchContext
    private lateinit var youtubeSettingFile: File
    private lateinit var rvxSettingFile: File

    fun setContext(context: ResourcePatchContext) {
        this.context = context
        this.youtubeSettingFile = context[YOUTUBE_SETTINGS_PATH]
        this.rvxSettingFile = context[RVX_PREFERENCE_PATH]
    }

    fun getContext() = context

    var restoreOldSplashAnimationIncluded = false

    private var iconType = "default"
    fun getIconType() = iconType

    fun updatePackageName(newPackageName: String) {
        youtubeSettingFile.writeText(
            youtubeSettingFile.readText()
                .replace(
                    "android:targetPackage=\"$YOUTUBE_PACKAGE_NAME",
                    "android:targetPackage=\"$newPackageName"
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

    fun addPreference(settingArray: Array<String>) {
        settingArray.forEach preferenceLoop@{ preference ->
            rvxSettingFile.writeText(
                this.rvxSettingFile.readText()
                    .replace("<!-- $preference", "")
                    .replace("$preference -->", "")
            )
        }
    }

    fun addPreference(patch: PatchList) {
        patch.included = true
        updatePatchStatus(patch.title.replace(" for YouTube", ""))
    }

    fun addPreference(settingArray: Array<String>, patch: PatchList) {
        addPreference(settingArray)
        addPreference(patch)
    }

    fun updatePatchStatus(patchTitle: String) {
        updatePatchStatusSettings(patchTitle, "@string/revanced_patches_included")
    }

    fun updatePatchStatusIcon(iconName: String) {
        iconType = iconName
        updatePatchStatusSettings("Icon", "@string/revanced_icon_$iconName")
    }

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

    fun addPreferenceFragment(
        key: String,
        insertKey: String,
        targetClass: String,
    ) = context.apply {
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
                                            YOUTUBE_PACKAGE_NAME
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

        // Modify the manifest to enhance TargetActivity behavior:
        // 1. Add a data intent filter with MIME type "text/plain".
        //    Some devices crash if undeclared data is passed to an intent,
        //    and this change appears to fix the issue.
        // 2. Add android:configChanges="orientation|screenSize|keyboardHidden".
        //    This prevents the activity from being recreated on configuration changes
        //    (e.g., screen rotation), preserving its current state and fragment.
        // 3. In Android 16+, the default value for the 'android:enableOnBackInvokedCallback' flag is 'true'.
        //    According to the 'Unsupported platform APIs, but unable to migrate' section of the Android documentation,
        //    Projects that don't use AndroidX APIs are recommended to change the 'android:enableOnBackInvokedCallback' attribute in the <activity> tag to 'false':
        //    https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture#update-custom
        document("AndroidManifest.xml").use { document ->
            val activityElement = document.childNodes.findElementByAttributeValueOrThrow(
                "android:name",
                targetClass,
            )

            if (!activityElement.hasAttribute("android:configChanges")) {
                activityElement.setAttribute(
                    "android:configChanges",
                    "keyboardHidden|orientation|screenSize"
                )
            }

            activityElement.setAttribute(
                "android:enableOnBackInvokedCallback",
                "false"
            )

            val mimeType = document.createElement("data")
            mimeType.setAttribute("android:mimeType", "text/plain")

            val intentFilter = document.createElement("intent-filter")
            intentFilter.appendChild(mimeType)

            activityElement.appendChild(intentFilter)
        }
    }
}
