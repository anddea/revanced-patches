package app.revanced.patches.music.general.branding.icon

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.util.resources.IconHelper.customIconMusic

@Patch(
    name = "Custom branding icon Revancify Blue",
    description = "Changes the YouTube Music launcher icon to Revancify Blue.",
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.15.52",
                "6.20.51",
                "6.23.55",
                "6.25.53"
            ]
        )
    ]
)
@Suppress("unused")
object CustomBrandingIconRevancifyBluePatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        context.customIconMusic("revancify-blue")

    }

}
