package app.revanced.patches.music.misc.splash

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.misc.splash.fingerprints.CairoSplashAnimationConfigFingerprint
import app.revanced.patches.music.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.literalInstructionBooleanHook

@Patch(
    name = "Disable Cairo splash animation",
    description = "Adds an option to disable Cairo splash animation.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "7.06.54",
                "7.17.51",
            ]
        )
    ]
)
@Suppress("unused")
object CairoSplashAnimationPatch : BytecodePatch(
    setOf(CairoSplashAnimationConfigFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        CairoSplashAnimationConfigFingerprint.result?.let {
            CairoSplashAnimationConfigFingerprint.literalInstructionBooleanHook(
                45635386,
                "$MISC_PATH/CairoSplashAnimationPatch;->disableCairoSplashAnimation(Z)Z"
            )

            SettingsPatch.addSwitchPreference(
                CategoryType.MISC,
                "revanced_disable_cairo_splash_animation",
                "false"
            )

        }
            ?: println("WARNING: This patch is not supported in this version. Use YouTube Music 7.06.54 or later.")

    }
}
