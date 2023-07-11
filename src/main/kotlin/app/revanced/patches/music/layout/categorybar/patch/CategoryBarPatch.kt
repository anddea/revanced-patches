package app.revanced.patches.music.layout.categorybar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.layout.categorybar.fingerprints.ChipCloudFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide category bar")
@Description("Hides the music category bar at the top of the homepage.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
@Version("0.0.1")
class CategoryBarPatch : BytecodePatch(
    listOf(ChipCloudFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ChipCloudFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static { v$targetRegister }, $MUSIC_LAYOUT->hideCategoryBar(Landroid/view/View;)V"
                )
            }
        } ?: return ChipCloudFingerprint.toErrorResult()

        SettingsPatch.addMusicPreference(
            CategoryType.LAYOUT,
            "revanced_hide_category_bar",
            "true"
        )

        return PatchResultSuccess()
    }
}
