package app.revanced.patches.shared.patch.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import app.revanced.util.resources.ResourceUtils.copyXmlNode

/**
 * Abstract settings resource patch
 *
 * @param sourceDirectory Source directory to copy the preference template from
 * @param sourceHostDirectory Source directory to copy the preference template from
 */
abstract class AbstractSettingsResourcePatch(
    private val sourceDirectory: String,
    private val sourceHostDirectory: String,
    private val isYouTube: Boolean,
) : ResourcePatch {
    override fun execute(context: ResourceContext) {
        /**
         * Copy strings
         */
        context.copyXmlNode(sourceHostDirectory, "values/strings.xml", "resources")

        /**
         * Initialize ReVanced Settings
         */
        if (isYouTube)
            context.copyResources(
                sourceDirectory,
                ResourceUtils.ResourceGroup("xml", "revanced_prefs.xml")
            )

    }
}