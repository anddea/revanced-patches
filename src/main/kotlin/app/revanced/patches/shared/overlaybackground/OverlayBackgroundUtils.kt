package app.revanced.patches.shared.overlaybackground

import app.revanced.patcher.data.ResourceContext
import app.revanced.util.doRecursively
import org.w3c.dom.Element

@Suppress("DEPRECATION")
object OverlayBackgroundUtils {
    internal fun ResourceContext.removeOverlayBackground(
        files: Array<String>,
        targetId: Array<String>,
    ) {
        files.forEach { file ->
            val targetXmlPath = this["res"].resolve("layout").resolve(file)

            if (targetXmlPath.exists()) {
                targetId.forEach { identifier ->
                    this.xmlEditor["res/layout/$file"].use { editor ->
                        editor.file.doRecursively {
                            arrayOf("height", "width").forEach replacement@{ replacement ->
                                if (it !is Element) return@replacement

                                if (it.attributes.getNamedItem("android:id")?.nodeValue?.endsWith(identifier) == true) {
                                    it.getAttributeNode("android:layout_$replacement")?.let { attribute ->
                                        attribute.textContent = "0.0dip"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

