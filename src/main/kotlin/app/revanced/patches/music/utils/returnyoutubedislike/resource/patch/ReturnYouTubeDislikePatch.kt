package app.revanced.patches.music.utils.returnyoutubedislike.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.returnyoutubedislike.bytecode.patch.ReturnYouTubeDislikeBytecodePatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.MusicResourceHelper.hookPreference
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources

@Patch
@Name("Return YouTube Dislike")
@Description("Shows the dislike count of videos using the Return YouTube Dislike API.")
@DependsOn(
    [
        ReturnYouTubeDislikeBytecodePatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
class ReturnYouTubeDislikePatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        /**
         * Copy preference
         */
        arrayOf(
            ResourceUtils.ResourceGroup(
                "xml",
                "returnyoutubedislike_prefs.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("music/returnyoutubedislike", resourceGroup)
        }

        /**
         * Hook RYD preference
         */
        context.hookPreference(
            "revanced_ryd_settings",
            "com.google.android.apps.youtube.music.settings.fragment.AdvancedPrefsFragmentCompat"
        )

        val publicFile = context["res/values/public.xml"]

        publicFile.writeText(
            publicFile.readText()
                .replace(
                    "\"advanced_prefs_compat\"",
                    "\"returnyoutubedislike_prefs\""
                )
        )

    }
}