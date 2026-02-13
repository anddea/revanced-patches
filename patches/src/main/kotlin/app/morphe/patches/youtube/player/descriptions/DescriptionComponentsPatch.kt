package app.morphe.patches.youtube.player.descriptions

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.engagement.engagementPanelHookPatch
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.fix.hype.hypeButtonIconPatch
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.DESCRIPTION_COMPONENTS
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_18_49_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_05_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.recyclerview.recyclerViewTreeObserverHook
import app.morphe.patches.youtube.utils.recyclerview.recyclerViewTreeObserverPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.rollingNumberTextViewAnimationUpdateFingerprint
import app.morphe.patches.youtube.utils.rollingNumberTextViewFingerprint
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/DescriptionsFilter;"

@Suppress("unused")
val descriptionComponentsPatch = bytecodePatch(
    DESCRIPTION_COMPONENTS.title,
    DESCRIPTION_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        playerTypeHookPatch,
        recyclerViewTreeObserverPatch,
        engagementPanelHookPatch,
        hypeButtonIconPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "SETTINGS: DESCRIPTION_COMPONENTS"
        )

        // region patch for disable rolling number animation

        // RollingNumber is applied to YouTube v18.49.37+.
        // In order to maintain compatibility with YouTube v18.48.39 or previous versions,
        // This patch is applied only to the version after YouTube v18.49.37.
        if (is_18_49_or_greater) {
            rollingNumberTextViewAnimationUpdateFingerprint.matchOrThrow(
                rollingNumberTextViewFingerprint
            ).let {
                it.method.apply {
                    val freeRegister = implementation!!.registerCount - parameters.size - 2
                    val imageSpanIndex = it.instructionMatches.first().index
                    val setTextIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.name == "setText"
                    }
                    addInstruction(setTextIndex, "nop")
                    addInstructionsWithLabels(
                        imageSpanIndex, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableRollingNumberAnimations()Z
                            move-result v$freeRegister
                            if-nez v$freeRegister, :disable_animations
                            """, ExternalLabel("disable_animations", getInstruction(setTextIndex))
                    )
                }
            }

            settingArray += "SETTINGS: DISABLE_ROLLING_NUMBER_ANIMATIONS"
        }

        // endregion

        // region patch for disable video description interaction and expand video description

        if (is_19_05_or_greater) {
            textViewComponentFingerprint.methodOrThrow().apply {
                val insertIndex = indexOfTextIsSelectableInstruction(this)
                val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

                replaceInstruction(
                    insertIndex,
                    "invoke-static {v${insertInstruction.registerC}, v${insertInstruction.registerD}}, " +
                            "$PLAYER_CLASS_DESCRIPTOR->disableVideoDescriptionInteraction(Landroid/widget/TextView;Z)V"
                )
            }

            recyclerViewTreeObserverHook("$PLAYER_CLASS_DESCRIPTOR->onVideoDescriptionCreate(Landroid/support/v7/widget/RecyclerView;)V")

            settingArray += "SETTINGS: DESCRIPTION_INTERACTION"
        }

        // endregion

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, DESCRIPTION_COMPONENTS)

        // endregion

    }
}
