package app.revanced.patches.youtube.misc.overridespeed.resource.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.util.resources.ResourceUtils.copyXmlNode

@Name("override-speed-hook-resource-patch")
@YouTubeCompatibility
@Version("0.0.1")
class OverrideSpeedHookResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         * Copy arrays
         */
        context.copyXmlNode("youtube/speed/host", "values/arrays.xml", "resources")

        return PatchResultSuccess()
    }
}
