package app.revanced.patches.music.misc.optimizeresource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Patch
@Name("Optimize resource")
@Description("Remove unnecessary resources.")
@MusicCompatibility
class OptimizeResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        val relativePath = "raw/third_party_licenses"

        Files.copy(
            this.javaClass.classLoader.getResourceAsStream("youtube/resource/$relativePath")!!,
            context["res"].resolve(relativePath).toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )

    }
}
