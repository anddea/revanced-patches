package app.revanced.patches.music.utils.sponsorblock

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.intenthook.IntentHookPatch
import app.revanced.patches.music.utils.settings.ResourceUtils
import app.revanced.patches.music.utils.settings.ResourceUtils.hookPreference
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources

@Patch(
    name = "SponsorBlock",
    description = "Adds options to enable and configure SponsorBlock, which can skip undesired video segments such as non-music sections.",
    dependencies = [
        IntentHookPatch::class,
        SettingsPatch::class,
        SponsorBlockBytecodePatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object SponsorBlockPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        /**
         * Copy preference
         */
        arrayOf(
            ResourceGroup(
                "xml",
                "sponsorblock_prefs.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("music/sponsorblock", resourceGroup)
        }

        /**
         * Hook SponsorBlock preference
         */
        context.hookPreference(
            "revanced_sponsorblock_settings",
            "com.google.android.apps.youtube.music.settings.fragment.AdvancedPrefsFragmentCompat"
        )

        val publicFile = context["res/values/public.xml"]

        publicFile.writeText(
            publicFile.readText()
                .replace(
                    "\"advanced_prefs_compat\"",
                    "\"sponsorblock_prefs\""
                )
        )

        context["res/xml/sponsorblock_prefs.xml"].writeText(
            context["res/xml/sponsorblock_prefs.xml"].readText()
                .replace(
                    "\"com.google.android.apps.youtube.music\"",
                    "\"" + ResourceUtils.targetPackage + "\""
                )
        )

    }
}