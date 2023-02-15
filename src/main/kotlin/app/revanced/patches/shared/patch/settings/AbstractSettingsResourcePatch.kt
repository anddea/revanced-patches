package app.revanced.patches.shared.patch.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.util.resources.ResourceHelper
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import app.revanced.util.resources.ResourceUtils.copyXmlNode

/**
 * Abstract settings resource patch
 *
 * @param sourceDirectory Source directory to copy the preference template from
 * @param sourcehostDirectory Source directory to copy the preference template from
 */
abstract class AbstractSettingsResourcePatch(
    private val sourceDirectory: String,
    private val sourcehostDirectory: String,
    private val isYouTube: Boolean,
) : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        /*
         * used for self-restart
         */
        context.xmlEditor["AndroidManifest.xml"].use { editor ->
            editor.file.getElementsByTagName("manifest").item(0).also {
                it.appendChild(it.ownerDocument.createElement("uses-permission").also { element ->
                    element.setAttribute("android:name", "android.permission.SCHEDULE_EXACT_ALARM")
                })
            }
        }

        /*
         * Copy strings
         */
        context.copyXmlNode(sourcehostDirectory, "values/strings.xml", "resources")

        /* initialize ReVanced Settings */
        if (isYouTube == true) {
            context.copyResources(sourceDirectory, ResourceUtils.ResourceGroup("xml", "revanced_prefs.xml"))
        }

        return PatchResultSuccess()
    }
}