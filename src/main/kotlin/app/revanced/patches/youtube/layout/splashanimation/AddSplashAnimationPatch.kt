package app.revanced.patches.youtube.layout.splashanimation

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import kotlin.io.path.exists

@Suppress("DEPRECATION", "unused")
object AddSplashAnimationPatch : ResourcePatch(
    name = "Add splash animation",
    description = "Adds old style splash animation.",
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        val resDirectory = context["res"]
        val targetXml = resDirectory.resolve("drawable").resolve("avd_anim.xml").toPath()

        /**
         * avd_anim.xml removed from YouTube v18.19.36+
         */
        if (!targetXml.exists()) {

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
}