package app.revanced.patches.youtube.general.suggestions.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.patch.litho.ComponentParserPatch.Companion.emptyComponentLabel
import app.revanced.patches.youtube.general.suggestions.fingerprints.BreakingNewsFingerprint
import app.revanced.patches.youtube.general.suggestions.fingerprints.SuggestionContentsBuilderFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PATCHES_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide suggestions shelf")
@Description("Hides the suggestions shelf.")
@DependsOn(
    [
        LithoFilterPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class SuggestionsShelfPatch : BytecodePatch(
    listOf(
        BreakingNewsFingerprint,
        SuggestionContentsBuilderFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Only used to tablet layout and the old UI components.
         */
        BreakingNewsFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $FILTER_CLASS_DESCRIPTOR->hideBreakingNewsShelf(Landroid/view/View;)V"
                )
            }
        } ?: throw BreakingNewsFingerprint.exception

        /**
         * Target method only removes the horizontal video shelf's content in the feed.
         * Since the header of the horizontal video shelf is not removed, it should be removed through the SuggestionsShelfFilter
         */
        SuggestionContentsBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    2, """
                        invoke-static/range {p2 .. p2}, $FILTER_CLASS_DESCRIPTOR->filterSuggestionsShelfSubComponents(Ljava/lang/Object;)Z
                        move-result v0
                        if-eqz v0, :show
                        """ + emptyComponentLabel, ExternalLabel("show", getInstruction(2))
                )
            }
        } ?: throw SuggestionContentsBuilderFingerprint.exception

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)


        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_SUGGESTIONS_SHELF"
            )
        )

        SettingsPatch.updatePatchStatus("hide-suggestions-shelf")

    }

    private companion object {
        private const val FILTER_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/ads/SuggestionsShelfFilter;"
    }
}
