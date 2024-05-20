package app.revanced.patches.youtube.shorts.dimming

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import org.w3c.dom.Element

@Suppress("Deprecation", "unused")
object ForceDisableShortsDimPatch : ResourcePatch(
    name = "Force disable Shorts dim",
    description = "Hide the dimming effect on the top and bottom of Shorts video at compile time.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {
        val hide = "0.0dip"

        fun hideLayoutAttributes(layoutFile: String, targetId: String) {
            context.xmlEditor[layoutFile].use { editor ->
                editor.file.doRecursively { node ->
                    if (node !is Element) return@doRecursively

                    when (node.getAttributeNode("android:id")?.textContent) {
                        targetId -> {
                            node.apply {
                                setAttribute("android:layout_height", hide)
                                setAttribute("android:layout_width", hide)
                            }
                        }
                    }
                }
            }
        }

        hideLayoutAttributes("res/layout/reel_player_overlay_scrims.xml", "@id/reel_player_overlay_v2_scrims_vertical")
        hideLayoutAttributes("res/layout/reel_watch_fragment.xml", "@id/reel_scrim_shorts_while_top")

        SettingsPatch.updatePatchStatus("Force disable Shorts dim")
    }
}
