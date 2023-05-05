package app.revanced.patches.youtube.layout.seekbar.seekbarcolor.resource.patch

import app.revanced.extensions.doRecursively
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.options.PatchOptions
import app.revanced.patches.youtube.layout.seekbar.seekbarcolor.bytecode.patch.SeekbarColorBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import org.w3c.dom.Element

@Patch
@Name("custom-seekbar-color")
@Description("Change seekbar color and progressbar color.")
@DependsOn(
    [
        PatchOptions::class,
        SeekbarColorBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ThemePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.xmlEditor[RESOURCE_FILE_PATH].use {
            it.file.doRecursively {
                arrayOf("color").forEach replacement@{ replacement ->
                    if (it !is Element) return@replacement

                    it.getAttributeNode("android:$replacement")?.let { attribute ->
                        if (attribute.textContent.startsWith("@color/"))
                            attribute.textContent = PatchOptions.resumedProgressBarColor!!
                    }
                }
            }
        }

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: CUSTOM_SEEKBAR_COLOR"
            )
        )

        SettingsPatch.updatePatchStatus("custom-seekbar-color")

        return PatchResultSuccess()
    }
    private companion object {
        const val RESOURCE_FILE_PATH = "res/drawable/resume_playback_progressbar_drawable.xml"
    }
}
