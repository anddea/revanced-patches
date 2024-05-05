package app.revanced.patches.youtube.layout.splashanimation

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import kotlin.io.path.exists

@Patch(
    name = "Add splash animation",
    description = "Adds old style splash animation.",
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.43",
                "19.12.41",
                "19.13.37",
                "19.14.43",
                "19.15.36",
                "19.16.38"
            ]
        )
    ]
)
@Suppress("unused")
object AddSplashAnimationPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        val resDirectory = context["res", false]
        val targetXml = resDirectory.resolve("drawable").resolve("avd_anim.xml").toPath()

        /**
         * avd_anim.xml removed from YouTube v18.19.36+
         */
        if (targetXml.exists()) return

        /**
         * merge Splash animation drawables to main drawables
         * extract from YouTube v18.18.39
         */
        arrayOf(
            ResourceGroup(
                "drawable",
                "\$\$avd_anim__1__0.xml",
                "\$\$avd_anim__1__1.xml",
                "\$\$avd_anim__2__0.xml",
                "\$\$avd_anim__2__1.xml",
                "\$\$avd_anim__3__0.xml",
                "\$\$avd_anim__3__1.xml",
                "\$avd_anim__0.xml",
                "\$avd_anim__1.xml",
                "\$avd_anim__2.xml",
                "\$avd_anim__3.xml",
                "\$avd_anim__4.xml",
                "avd_anim.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/splashscreen", resourceGroup)
        }

        /**
         * merge Splash animation styles to main styles
         * extract from YouTube v18.18.39
         */
        context.copyXmlNode("youtube/splashscreen", "values-v31/styles.xml", "resources")

    }
}