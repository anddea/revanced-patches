package app.revanced.patches.music.navigation.black

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.navigation.black.fingerprints.TabLayoutFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_NAVIGATION
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Enable black navigation bar",
    description = "Sets the navigation bar color to black.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ]
)
@Suppress("unused")
object BlackNavigationBarPatch : BytecodePatch(
    setOf(TabLayoutFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        TabLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {}, $MUSIC_NAVIGATION->enableBlackNavigationBar()I
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw TabLayoutFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_enable_black_navigation_bar",
            "true"
        )

    }
}
