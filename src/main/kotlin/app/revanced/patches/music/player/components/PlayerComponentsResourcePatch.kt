@file:Suppress("DEPRECATION")

package app.revanced.patches.music.player.components

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.DomFileEditor
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.adoptChild
import app.revanced.util.doRecursively
import app.revanced.util.insertNode
import org.w3c.dom.Element
import java.io.Closeable

@Patch(dependencies = [SettingsPatch::class])
object PlayerComponentsResourcePatch : ResourcePatch(), Closeable {
    private const val IMAGE_VIEW_TAG_NAME =
        "com.google.android.libraries.youtube.common.ui.TouchImageView"
    private const val NEXT_BUTTON_VIEW_ID =
        "mini_player_next_button"
    private const val PREVIOUS_BUTTON_VIEW_ID =
        "mini_player_previous_button"

    private lateinit var targetXmlEditor: DomFileEditor
    private var shouldAddPreviousButton = true

    override fun execute(context: ResourceContext) {
        targetXmlEditor = context.xmlEditor["res/layout/watch_while_layout.xml"]

        // Since YT Music v6.42.51,the resources for the next button have been removed, we need to add them manually.
        if (SettingsPatch.upward0642) {
            context.replaceId(false)
            insertNode(false)
        }
        context.replaceId(true)
        insertNode(true)
    }

    override fun close() = targetXmlEditor.close()

    private fun insertNode(isPreviousButton: Boolean) {
        targetXmlEditor.file.doRecursively loop@{ node ->
            if (node !is Element) return@loop

            node.getAttributeNode("android:id")?.let { attribute ->
                if (isPreviousButton) {
                    if (attribute.textContent == "@id/mini_player_play_pause_replay_button" && shouldAddPreviousButton) {
                        node.insertNode(IMAGE_VIEW_TAG_NAME, node) {
                            setPreviousButtonNodeAttribute()
                        }
                    }
                } else {
                    if (attribute.textContent == "@id/mini_player") {
                        node.adoptChild(IMAGE_VIEW_TAG_NAME) {
                            setNextButtonNodeAttribute()
                        }
                    }
                }
            }
        }
    }

    private fun ResourceContext.replaceId(isPreviousButton: Boolean) {
        val publicFile = this["res/values/public.xml"]

        if (isPreviousButton) {
            publicFile.writeText(
                publicFile.readText()
                    .replace(
                        "\"TOP_END\"",
                        "\"$PREVIOUS_BUTTON_VIEW_ID\""
                    )
            )
        } else {
            publicFile.writeText(
                publicFile.readText()
                    .replace(
                        "\"TOP_START\"",
                        "\"$NEXT_BUTTON_VIEW_ID\""
                    )
            )
        }
    }

    private fun Element.setNextButtonNodeAttribute() {
        mapOf(
            "android:id" to "@id/$NEXT_BUTTON_VIEW_ID",
            "android:padding" to "@dimen/item_medium_spacing",
            "android:layout_width" to "@dimen/remix_generic_button_size",
            "android:layout_height" to "@dimen/remix_generic_button_size",
            "android:src" to "@drawable/music_player_next",
            "android:scaleType" to "fitCenter",
            "android:contentDescription" to "@string/accessibility_next",
            "style" to "@style/MusicPlayerButton"
        ).forEach { (k, v) ->
            setAttribute(k, v)
        }
    }

    private fun Element.setPreviousButtonNodeAttribute() {
        mapOf(
            "android:id" to "@id/$PREVIOUS_BUTTON_VIEW_ID",
            "android:padding" to "@dimen/item_medium_spacing",
            "android:layout_width" to "@dimen/remix_generic_button_size",
            "android:layout_height" to "@dimen/remix_generic_button_size",
            "android:src" to "@drawable/music_player_prev",
            "android:scaleType" to "fitCenter",
            "android:contentDescription" to "@string/accessibility_previous",
            "style" to "@style/MusicPlayerButton"
        ).forEach { (k, v) ->
            setAttribute(k, v)
        }
        shouldAddPreviousButton = false
    }
}