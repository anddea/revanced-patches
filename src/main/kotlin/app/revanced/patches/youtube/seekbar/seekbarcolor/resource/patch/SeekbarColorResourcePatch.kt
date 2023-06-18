package app.revanced.patches.youtube.seekbar.seekbarcolor.resource.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import org.w3c.dom.Element

@Name("custom-seekbar-color-resource-patch")
@YouTubeCompatibility
@Version("0.0.1")
class SeekbarColorResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.xmlEditor["res/drawable/resume_playback_progressbar_drawable.xml"].use {
            val layerList = it.file.getElementsByTagName("layer-list").item(0) as Element
            val progressNode = layerList.getElementsByTagName("item").item(1) as Element
            if (!progressNode.getAttributeNode("android:id").value.endsWith("progress")) {
                return PatchResultError("Could not find progress bar")
            }
            val scaleNode = progressNode.getElementsByTagName("scale").item(0) as Element
            val shapeNode = scaleNode.getElementsByTagName("shape").item(0) as Element
            val replacementNode = it.file.createElement(
                "app.revanced.integrations.patches.utils.ProgressBarDrawable")
            scaleNode.replaceChild(replacementNode, shapeNode)
        }

        return PatchResultSuccess()
    }
}
