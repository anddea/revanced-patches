package app.revanced.patches.youtube.misc.optimize.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.misc.optimize.patch.RedundantResourcePatch
import app.revanced.patches.youtube.misc.optimize.patch.MissingTranslationPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator

@Patch
@Name("optimize-resource")
@DependsOn(
    [
        RedundantResourcePatch::class,
        MissingTranslationPatch::class,
        SettingsPatch::class
    ]
)
@Description("Removes duplicate resources and adds missing translation files from YouTube.")
@YouTubeCompatibility
@Version("0.0.1")
class OptimizeResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        ResourceHelper.patchSuccess(
            context,
            "optimize-resource"
        )

        return PatchResultSuccess()
    }
}
