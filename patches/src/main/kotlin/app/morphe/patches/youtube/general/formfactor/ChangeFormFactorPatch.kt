/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.formfactor

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import app.morphe.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.morphe.patches.shared.spoof.guide.addClientInfoHook
import app.morphe.patches.shared.spoof.guide.spoofClientGuideEndpointPatch
import app.morphe.patches.youtube.player.action.restoreOldVideoActionBarPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.navigation.navigationBarHookPatch
import app.morphe.patches.youtube.utils.patch.PatchList.CHANGE_FORM_FACTOR
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_20_00_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_31_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findFreeRegister
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/ChangeFormFactorPatch;"

@Suppress("unused")
val changeFormFactorPatch = bytecodePatch(
    CHANGE_FORM_FACTOR.title,
    CHANGE_FORM_FACTOR.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        playerTypeHookPatch,
        navigationBarHookPatch,
        spoofClientGuideEndpointPatch,
        restoreOldVideoActionBarPatch,
        versionCheckPatch,
    )

    execute {

        val formFactorEnumClass = formFactorEnumConstructorFingerprint.originalClassDef.type

        createPlayerRequestBodyWithModelFingerprint.methodOrThrow().apply {
            val ordinalIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IGET &&
                        reference?.definingClass == formFactorEnumClass &&
                        reference.type == "I"
            }
            val ordinalRegister = getInstruction<TwoRegisterInstruction>(ordinalIndex).registerA

            // This patch changes the 'clientFormFactor' value to a different value
            addInstructions(
                ordinalIndex + 1, """
                    invoke-static {v$ordinalRegister}, $EXTENSION_CLASS_DESCRIPTOR->getFormFactor(I)I
                    move-result v$ordinalRegister
                    """
            )

            val clientFormFactorOrdinalIndex =
                indexOfFirstInstructionOrThrow(ordinalIndex - 1) {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IPUT &&
                            reference?.type == "I" &&
                            reference.definingClass == CLIENT_INFO_CLASS_DESCRIPTOR
                }
            val clientFormFactorOrdinalReference =
                getInstruction<ReferenceInstruction>(clientFormFactorOrdinalIndex).reference

            // Changing 'clientFormFactor' in all requests will also affect the navigation bar
            // If 'clientFormFactor' is 'AUTOMOTIVE_FORM_FACTOR', the 'Shorts' button in the navigation bar will change to 'Explore'
            // To fix this side effect, requests to the '/guide' endpoint, which are related to navigation buttons, use the original 'clientFormFactor'
            addClientInfoHook(
                "patch_setClientFormFactor",
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getFormFactor()I
                    move-result v2
                    iput v2, v1, $clientFormFactorOrdinalReference
                    """
            )
        }

        widthDpUIFingerprint.let {
            it.method.apply {
                val index = it.instructionMatches.first().index
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->getWidthDp(I)I
                        move-result v$register
                        """
                )
            }
        }

        // region add settings

        val preferences = mutableListOf(
            "PREFERENCE_SCREEN: GENERAL",
            "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
            "SETTINGS: CHANGE_FORM_FACTOR",
        )
        if (is_20_31_or_greater) {
            preferences += "SETTINGS: TABLET_LAYOUT_IN_PLAYER"
        }
        addPreference(preferences.toTypedArray(), CHANGE_FORM_FACTOR)

        // endregion

        if (is_20_00_or_greater) {
            RepeatedItemSectionRendererFingerprint.let {
                it.method.apply {
                    val match = it.instructionMatches[1]
                    val index = match.index
                    val instruction = match.instruction
                    val listRegister = instruction.registersUsed[0]
                    val listIndexRegister = instruction.registersUsed[1]
                    val free = findFreeRegister(index, listRegister, listIndexRegister)

                    addInstructionsWithLabels(
                        index,
                        """
                        invoke-static { v$listRegister, v$listIndexRegister }, $EXTENSION_CLASS_DESCRIPTOR->checkItemSectionRenderer(Ljava/util/List;I)Z
                        move-result v$free
                        if-nez v$free, :empty_list_check
                        return-void
                        :empty_list_check
                        nop
                    """
                    )
                }
            }
        }
    }
}
