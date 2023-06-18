package app.revanced.patches.youtube.layout.general.categorybar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.categorybar.fingerprints.FilterBarHeightFingerprint
import app.revanced.patches.youtube.layout.general.categorybar.fingerprints.RelatedChipCloudFingerprint
import app.revanced.patches.youtube.layout.general.categorybar.fingerprints.SearchResultsChipBarFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-category-bar")
@Description("Hide the category bar at the top of the feed and at the top of related videos.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class CategoryBarPatch : BytecodePatch(
    listOf(
        FilterBarHeightFingerprint,
        RelatedChipCloudFingerprint,
        SearchResultsChipBarFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /**
         * Category Bar in feed
         * Home feed and subscriptions feed
         */
        FilterBarHeightFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$register}, $GENERAL->hideCategoryBarInFeed(I)I
                        move-result v$register
                        """
                )
            }
        } ?: return FilterBarHeightFingerprint.toErrorResult()

        /**
         * Category Bar in related video
         */
        RelatedChipCloudFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "invoke-static {v$register}, $GENERAL->hideCategoryBarInRelatedVideo(Landroid/view/View;)V"
                )
            }
        } ?: return RelatedChipCloudFingerprint.toErrorResult()

        /**
         * Category Bar in search results
         */
        SearchResultsChipBarFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex - 2
                val register = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$register}, $GENERAL->hideCategoryBarInSearchResults(I)I
                        move-result v$register
                        """
                )
            }
        } ?: return SearchResultsChipBarFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_CATEGORY_BAR"
            )
        )

        SettingsPatch.updatePatchStatus("hide-category-bar")

        return PatchResultSuccess()
    }
}
