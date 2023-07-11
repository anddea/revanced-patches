package app.revanced.patches.microg.layout.hideicon.patch

import app.revanced.extensions.doRecursively
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.microg.utils.annotations.MicroGCompatibility
import org.w3c.dom.Element

@Patch(false)
@Name("Hide icon from launcher")
@Description("Hide MicroG icon from launcher.")
@MicroGCompatibility
@Version("0.0.1")
class HideIconPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.xmlEditor["AndroidManifest.xml"].use { editor ->
            editor.file.doRecursively loop@{
                if (it !is Element) return@loop

                it.getAttributeNode("android:name")?.let { attribute ->
                    if (attribute.textContent == "android.intent.category.LAUNCHER") {
                        attribute.textContent = "android.intent.category.DEFAULT"
                    }
                }
            }
        }

        return PatchResultSuccess()
    }
}
