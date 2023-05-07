package app.revanced.patches.youtube.layout.seekbar.seekbarcolor.resource.patch

import app.revanced.extensions.doRecursively
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.seekbar.seekbarcolor.bytecode.patch.SeekbarColorBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import org.w3c.dom.Element

@Patch
@Name("custom-seekbar-color")
@Description("Change seekbar color and progressbar color.")
@DependsOn(
    [
        SeekbarColorBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ThemePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.xmlEditor["res/drawable/resume_playback_progressbar_drawable.xml"].use {
            it.file.doRecursively {
                arrayOf("color").forEach replacement@{ replacement ->
                    if (it !is Element) return@replacement

                    it.getAttributeNode("android:$replacement")?.let { attribute ->
                        if (attribute.textContent.startsWith("@color/"))
                            attribute.textContent = resumedProgressBarColor
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

    companion object : OptionsContainer() {
        var resumedProgressBarColor: String? by option(
            PatchOption.StringOption(
                key = "resumedProgressBarColor",
                default = "#ffff0000",
                title = "Resumed progressbar color",
                description = "Resumed progressbar color in playlists and history. Can be a hex color or a resource reference."
            )
        )
    }
}
