package app.revanced.patches.music.layout.blacknavigationbar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.layout.blacknavbar.fingerprints.TabLayoutFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-black-navigation-bar")
@Description("Sets the navigation bar color to black.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
@Version("0.0.1")
class BlackNavigationBarPatch : BytecodePatch(
    listOf(TabLayoutFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        TabLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {}, $MUSIC_LAYOUT->enableBlackNavigationBar()I
                        move-result v$targetRegister
                        """
                )
            }
        } ?: return TabLayoutFingerprint.toErrorResult()

        SettingsPatch.addMusicPreference(
            CategoryType.LAYOUT,
            "revanced_enable_black_navigation_bar",
            "true"
        )

        return PatchResultSuccess()
    }
}
