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
import app.revanced.util.integrations.Constants.GENERAL
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
         * Legacy code for old layout
         */
        BreakingNewsFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $GENERAL->hideBreakingNewsShelf(Landroid/view/View;)V"
                )
            }
        } ?: throw BreakingNewsFingerprint.exception

        /**
         * For new layout
         *
         * Target method only removes the horizontal video shelf's content in the feed.
         * Since the header of the horizontal video shelf is not removed, it must be removed through the low level filter
         */
        SuggestionContentsBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    2, """
                        invoke-static/range {p2 .. p2}, $GENERAL->hideSuggestionsShelf(Ljava/lang/Object;)Z
                        move-result v0
                        if-eqz v0, :show
                        """ + emptyComponentLabel, ExternalLabel("show", getInstruction(2))
                )
            }
        } ?: throw SuggestionContentsBuilderFingerprint.exception


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
}
