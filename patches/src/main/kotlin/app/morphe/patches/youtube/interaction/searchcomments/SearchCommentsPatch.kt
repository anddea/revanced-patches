/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3942
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.interaction.searchcomments

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.bridge.addBridge
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.youtube.utils.engagement.engagementPanelHookPatch
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.patch.PatchList.SEARCH_COMMENTS
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.utils.playservice.is_20_05_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.util.Utils.printWarn
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_FILTER =
    "Lapp/morphe/extension/youtube/patches/components/CommentsSearchFilter;"

private const val SECTION_LIST_ACCESS_INTERFACE =
    $$"Lapp/morphe/extension/youtube/patches/components/CommentsSearchFilter$SectionListAccess;"

@Suppress("unused")
val searchCommentsPatch = bytecodePatch(
    name = "Search comments",
    description = "Adds a button to the comments to show only the loaded comments that contain the searched words."
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        lithoFilterPatch,
        engagementPanelHookPatch,
        videoInformationPatch,
        versionCheckPatch,
        resourcePatch {
            execute {
                copyResources(
                    "searchcomments",
                    ResourceGroup("drawable", "morphe_search_comments_button.xml")
                )
            }
        }
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        // Older section-list controllers do not expose the list-view field used by the reload bridge.
        // Do not install the filter or expose a setting when its controller bridge cannot be added.
        if (!is_20_05_or_greater) {
            printWarn("\"Search comments\" is not supported in this version. Use YouTube 20.05 or greater.")
            return@execute
        }

        addPreference(
            arrayOf("PREFERENCE_SCREEN: GENERAL", "SETTINGS: SEARCH_COMMENTS"),
            SEARCH_COMMENTS,
        )

        addLithoFilter(EXTENSION_FILTER)

        // The search button is added next to the close button of the comments panel.
        PanelSubheaderFingerprint.let {
            it.method.apply {
                val removeAllViewsIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "removeAllViews"
                }

                val setVisibilityIndex = indexOfFirstInstructionOrThrow(removeAllViewsIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setVisibility"
                }

                val subheaderRegister = getInstruction<FiveRegisterInstruction>(setVisibilityIndex).registerC

                addInstruction(
                    setVisibilityIndex + 1,
                    "invoke-static { v$subheaderRegister }, $EXTENSION_FILTER->onPanelSubheaderShown(Landroid/view/View;)V"
                )
            }
        }

        // The comments are reloaded after a search, as when the sort order is changed,
        // so the search applies to all of them.
        val refreshMatches = SectionListRefreshFingerprint.instructionMatches
        val reloadContinuationType = refreshMatches.first().getFieldAccessed()
        val hasContinuationMethod = refreshMatches[1].getMethodCalled()
        val reloadMethod = refreshMatches.last().getMethodCalled()
        val listViewField = SectionListControllerConstructorFingerprint.instructionMatches.first().getFieldAccessed()

        mutableClassDefBy(listViewField.definingClass).apply {
            interfaces.add(SECTION_LIST_ACCESS_INTERFACE)
            addBridge(
                "patch_getListView", listOf(), "Landroid/view/View;", 2,
                """
                    iget-object v0, p0, $listViewField
                    return-object v0
                """
            )
            addBridge(
                "patch_reload", listOf(), "Z", 2,
                """
                    sget-object v0, $reloadContinuationType
                    invoke-virtual { p0, v0 }, $hasContinuationMethod
                    move-result v0
                    if-eqz v0, :no_reload_continuation
                    invoke-virtual { p0 }, $reloadMethod
                    :no_reload_continuation
                    return v0
                """
            )
        }

        SectionListControllerConstructorFingerprint.let {
            it.method.apply {
                addInstruction(
                    it.instructionMatches.last().index,
                    "invoke-static/range { p0 .. p0 }, $EXTENSION_FILTER->onSectionListCreated($SECTION_LIST_ACCESS_INTERFACE)V"
                )
            }
        }
    }
}
