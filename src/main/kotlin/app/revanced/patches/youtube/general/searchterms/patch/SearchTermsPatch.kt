package app.revanced.patches.youtube.general.searchterms.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.general.searchterms.fingerprints.SearchEndpointFingerprint
import app.revanced.patches.youtube.general.searchterms.fingerprints.SearchEndpointParentFingerprint
import app.revanced.patches.youtube.general.searchterms.fingerprints.SearchSuggestionEntryFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.SearchSuggestionEntry
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("hide-search-terms")
@Description("Hide trending searches and search history in the search bar.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SearchTermsPatch : BytecodePatch(
    listOf(
        SearchEndpointParentFingerprint,
        SearchSuggestionEntryFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SearchEndpointParentFingerprint.result?.let { parentResult ->
            SearchEndpointFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                    val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstruction(
                        targetIndex + 1,
                        "sput-boolean v$targetRegister, $GENERAL->isSearchWordEmpty:Z"
                    )
                }
            } ?: return SearchEndpointFingerprint.toErrorResult()
        } ?: return SearchEndpointParentFingerprint.toErrorResult()

        SearchSuggestionEntryFingerprint.result?.mutableMethod?.let {
            val targetIndex = it.getWideLiteralIndex(SearchSuggestionEntry) + 2
            val targetRegister = it.getInstruction<OneRegisterInstruction>(targetIndex).registerA

            it.addInstruction(
                targetIndex + 4,
                "invoke-static {v$targetRegister}, $GENERAL->hideSearchTerms(Landroid/view/View;)V"
            )

            it.addInstruction(
                targetIndex + 2,
                "invoke-static {v$targetRegister}, $GENERAL->hideSearchTerms(Landroid/view/View;)V"
            )
        } ?: return SearchSuggestionEntryFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_SEARCH_TERMS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-search-terms")

        return PatchResultSuccess()
    }
}