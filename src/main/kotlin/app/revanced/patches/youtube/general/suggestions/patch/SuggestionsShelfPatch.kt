package app.revanced.patches.youtube.general.suggestions.patch

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
import app.revanced.patches.youtube.general.suggestions.fingerprints.BreakingNewsFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.utils.navbarindex.patch.NavBarIndexHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("hide-suggestions-shelf")
@Description("Hides the suggestions shelf.")
@DependsOn(
    [
        LithoFilterPatch::class,
        NavBarIndexHookPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SuggestionsShelfPatch : BytecodePatch(
    listOf(BreakingNewsFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        BreakingNewsFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $GENERAL->hideBreakingNewsShelf(Landroid/view/View;)V"
                )
            }
        } ?: return BreakingNewsFingerprint.toErrorResult()

        /*
        SuggestionContentsBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    2, """
                        invoke-static/range {p2 .. p2}, $GENERAL->hideSuggestionsShelf(Ljava/lang/Object;)Z
                        move-result v0
                        if-eqz v0, :not_an_ad
                        """ + emptyComponentLabel, ExternalLabel("not_an_ad", getInstruction(2))
                )
            }
        } ?: return SuggestionContentsBuilderFingerprint.toErrorResult()
         */


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

        return PatchResultSuccess()
    }
}
