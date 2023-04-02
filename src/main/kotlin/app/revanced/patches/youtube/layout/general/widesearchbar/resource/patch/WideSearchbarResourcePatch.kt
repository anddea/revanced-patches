package app.revanced.patches.youtube.layout.general.widesearchbar.resource.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.shared.annotation.YouTubeCompatibility

@Name("enable-wide-searchbar-resource-patch")
@YouTubeCompatibility
@Version("0.0.1")
class WideSearchbarResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        context.xmlEditor["res/layout/action_bar_ringo_background.xml"].use { editor ->
            val document = editor.file

            with(document.getElementsByTagName("RelativeLayout").item(0)) {
                if (attributes.getNamedItem(FLAG) != null) return@with

                document.createAttribute(FLAG)
                    .apply { value = "8.0dip" }
                    .let(attributes::setNamedItem)

            }
        }

        return PatchResultSuccess()
    }

    private companion object {
        const val FLAG = "android:paddingStart"
    }
}
