package app.revanced.patches.youtube.general.categorybar.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.general.categorybar.fingerprints.FilterBarHeightFingerprint
import app.revanced.patches.youtube.general.categorybar.fingerprints.RelatedChipCloudFingerprint
import app.revanced.patches.youtube.general.categorybar.fingerprints.SearchResultsChipBarFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Hide category bar")
@Description("Hides the category bar in video feeds.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class CategoryBarPatch : BytecodePatch(
    listOf(
        FilterBarHeightFingerprint,
        RelatedChipCloudFingerprint,
        SearchResultsChipBarFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        FilterBarHeightFingerprint.patch<TwoRegisterInstruction> { register ->
            """
                invoke-static { v$register }, $GENERAL->hideCategoryBarInFeed(I)I
                move-result v$register
            """
        }

        RelatedChipCloudFingerprint.patch<OneRegisterInstruction>(1) { register ->
            "invoke-static { v$register }, " +
                    "$GENERAL->hideCategoryBarInRelatedVideo(Landroid/view/View;)V"
        }

        SearchResultsChipBarFingerprint.patch<OneRegisterInstruction>(-1, -2) { register ->
            """
                invoke-static { v$register }, $GENERAL->hideCategoryBarInSearchResults(I)I
                move-result v$register
            """
        }

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

    }

    private companion object {
        private fun <RegisterInstruction : OneRegisterInstruction> MethodFingerprint.patch(
            insertIndexOffset: Int = 0,
            hookRegisterOffset: Int = 0,
            instructions: (Int) -> String
        ) =
            result?.let {
                it.mutableMethod.apply {
                    val endIndex = it.scanResult.patternScanResult!!.endIndex

                    val insertIndex = endIndex + insertIndexOffset
                    val register =
                        getInstruction<RegisterInstruction>(endIndex + hookRegisterOffset).registerA

                    addInstructions(insertIndex, instructions(register))
                }
            } ?: throw exception
    }
}
