package app.revanced.patches.shared.voicesearch

import app.revanced.patcher.data.ResourceContext
import org.w3c.dom.Element

@Suppress("DEPRECATION")
object VoiceSearchUtils {
    private const val IMAGE_VIEW_TAG = "android.support.v7.widget.AppCompatImageView"
    private const val VOICE_SEARCH_ID = "@id/voice_search"

    internal fun ResourceContext.patchXml(
        paths: Array<String>,
        replacements: Array<String>
    ) {
        val resDirectory = this["res"]

        paths.forEach { path ->
            val targetXmlPath = resDirectory.resolve("layout").resolve(path)

            if (targetXmlPath.exists()) {
                this.xmlEditor["res/layout/$path"].use { editor ->
                    val document = editor.file
                    val imageViewTags = document.getElementsByTagName(IMAGE_VIEW_TAG)
                    List(imageViewTags.length) { imageViewTags.item(it) as Element }
                        .filter { it.getAttribute("android:id").equals(VOICE_SEARCH_ID) }
                        .forEach { node ->
                            replacements.forEach replacement@{ replacement ->
                                node.getAttributeNode("android:layout_$replacement")
                                    ?.let { attribute ->
                                        attribute.textContent = "0.0dip"
                                    }
                            }
                        }
                }
            }
        }
    }
}
