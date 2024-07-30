package app.revanced.patches.music.misc.splash

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.misc.splash.fingerprints.CairoSplashAnimationConfigFingerprint
import app.revanced.patches.music.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.literalInstructionBooleanHook

@Patch(
    name = "Enable Cairo splash animation",
    description = "Adds an option to enable Cairo splash animation.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "7.08.54",
                "7.12.51",
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
                "$MISC_PATH/CairoSplashAnimationPatch;->enableCairoSplashAnimation()Z"
            )

            SettingsPatch.addSwitchPreference(
                CategoryType.MISC,
                "revanced_enable_cairo_splash_animation",
                "false"
            )

        } ?: throw PatchException("WARNING: This patch is not supported in this version. Use YouTube Music 7.08.54 or later.")

    }
}
