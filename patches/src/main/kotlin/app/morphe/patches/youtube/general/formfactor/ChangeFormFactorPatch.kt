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

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import app.morphe.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.morphe.patches.shared.spoof.guide.addClientInfoHook
import app.morphe.patches.shared.spoof.guide.spoofClientGuideEndpointPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.navigation.navigationBarHookPatch
import app.morphe.patches.youtube.utils.patch.PatchList.CHANGE_FORM_FACTOR
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_20_00_or_greater
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findFreeRegister
import app.morphe.util.fingerprint.definingClassOrThrow
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
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
    )

    execute {

        val formFactorEnumClass = formFactorEnumConstructorFingerprint
            .definingClassOrThrow()

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

        widthDpUIFingerprint.matchOrThrow().let {
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

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: CHANGE_FORM_FACTOR",
                "SETTINGS: TABLET_LAYOUT_IN_PLAYER"
            ),
            CHANGE_FORM_FACTOR
        )

        // endregion

        val playerLithoElementsListFingerprint = Fingerprint(
            accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
            returnType = "V",
            strings = listOf("Number of sectionList models must be equal to the number of section states"),
            filters = listOf(
                fieldAccess(
                    opcode = Opcode.IGET_OBJECT,
                    type = "Ljava/util/List;",
                ),
                methodCall(
                    opcode = Opcode.INVOKE_INTERFACE,
                    smali = "Ljava/util/List;->get(I)Ljava/lang/Object;",
                    location = MatchAfterImmediately(),
                ),
                opcode(
                    Opcode.MOVE_RESULT_OBJECT,
                    location = MatchAfterImmediately(),
                ),
                opcode(
                    Opcode.CHECK_CAST,
                    location = MatchAfterImmediately(),
                ),
                opcode(
                    Opcode.INVOKE_VIRTUAL,
                    location = MatchAfterImmediately(),
                ),
                opcode(
                    Opcode.INSTANCE_OF,
                    location = MatchAfterImmediately(),
                ),
                opcode(
                    Opcode.IF_EQZ,
                    location = MatchAfterImmediately(),
                ),
                opcode(
                    Opcode.CHECK_CAST,
                    location = MatchAfterImmediately(),
                ),
                opcode(
                    Opcode.IGET_OBJECT,
                    location = MatchAfterImmediately(),
                ),
            ),
        )

        if (is_20_00_or_greater) {
            playerLithoElementsListFingerprint.let {
                it.method.apply {
                    val index = it.instructionMatches.first().index
                    val register = getInstruction<BuilderInstruction22c>(index).registerA
                    val free = findFreeRegister(index, register)

                    addInstructionsWithLabels(
                        index + 1,
                        """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->checkPlayerLithoElementsListSize(Ljava/util/List;)Z
                        move-result v$free
                        if-eqz v$free, :empty_list_check
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
