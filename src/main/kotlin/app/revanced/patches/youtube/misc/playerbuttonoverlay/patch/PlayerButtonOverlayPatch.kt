package app.revanced.patches.youtube.misc.playerbuttonoverlay.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.doRecursively
import app.revanced.shared.util.resources.ResourceHelper
import org.w3c.dom.Element

@Patch
@Name("remove-player-button-background")
@Description("Removes the background from the video player buttons.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class PlayerButtonOverlayPatch : ResourcePatch {
    private companion object {
        const val RESOURCE_FILE_PATH = "res/drawable/player_button_circle_background.xml"

        val replacements = arrayOf(
            "color"
        )
    }
    
    override fun execute(context: ResourceContext): PatchResult {
        context.xmlEditor[RESOURCE_FILE_PATH].use { editor ->
            editor.file.doRecursively { node ->
                replacements.forEach replacement@{ replacement ->
                    if (node !is Element) return@replacement

                    node.getAttributeNode("android:$replacement")?.let { attribute ->
                        attribute.textContent = "@android:color/transparent"
                    }
                }
            }
        }

        ResourceHelper.patchSuccess(
            context,
            "remove-player-button-background"
        )

        return PatchResultSuccess()
    }
}