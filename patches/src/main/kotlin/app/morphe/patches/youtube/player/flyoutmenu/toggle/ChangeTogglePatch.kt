package app.morphe.patches.youtube.player.flyoutmenu.toggle

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.patch.PatchList.CHANGE_PLAYER_FLYOUT_MENU_TOGGLES
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.resolvable
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val changeTogglePatch = bytecodePatch(
    CHANGE_PLAYER_FLYOUT_MENU_TOGGLES.title,
    CHANGE_PLAYER_FLYOUT_MENU_TOGGLES.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        fun changeToggleCinematicLightingHook() {
            val stableVolumeMethod = stableVolumeFingerprint.methodOrThrow()

            val stringReferenceIndex = stableVolumeMethod.indexOfFirstInstruction {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        (this as ReferenceInstruction).reference.toString()
                            .endsWith("(Ljava/lang/String;Ljava/lang/String;)V")
            }
            if (stringReferenceIndex < 0)
                throw PatchException("Target reference was not found in stableVolumeFingerprint.")

            val stringReference =
                stableVolumeMethod.getInstruction<ReferenceInstruction>(stringReferenceIndex).reference

            cinematicLightingFingerprint.methodOrThrow().apply {
                val iGetIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET &&
                            getReference<FieldReference>()?.definingClass == definingClass
                }
                val classRegister = getInstruction<TwoRegisterInstruction>(iGetIndex).registerB

                val stringIndex =
                    indexOfFirstStringInstructionOrThrow("menu_item_cinematic_lighting")

                val checkCastIndex =
                    indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.CHECK_CAST)
                val iGetObjectPrimaryIndex =
                    indexOfFirstInstructionReversedOrThrow(checkCastIndex, Opcode.IGET_OBJECT)
                val iGetObjectSecondaryIndex =
                    indexOfFirstInstructionOrThrow(checkCastIndex, Opcode.IGET_OBJECT)

                val checkCastReference =
                    getInstruction<ReferenceInstruction>(checkCastIndex).reference
                val iGetObjectPrimaryReference =
                    getInstruction<ReferenceInstruction>(iGetObjectPrimaryIndex).reference
                val iGetObjectSecondaryReference =
                    getInstruction<ReferenceInstruction>(iGetObjectSecondaryIndex).reference

                val invokeVirtualIndex =
                    indexOfFirstInstructionOrThrow(stringIndex, Opcode.INVOKE_VIRTUAL)
                val invokeVirtualInstruction =
                    getInstruction<FiveRegisterInstruction>(invokeVirtualIndex)
                val freeRegisterC = invokeVirtualInstruction.registerC
                val freeRegisterD = invokeVirtualInstruction.registerD
                val freeRegisterE = invokeVirtualInstruction.registerE

                val insertIndex = indexOfFirstInstructionOrThrow(stringIndex, Opcode.RETURN_VOID)

                addInstructionsWithLabels(
                    insertIndex, """
                        const/4 v$freeRegisterC, 0x1
                        invoke-static {v$freeRegisterC}, $PLAYER_CLASS_DESCRIPTOR->changeSwitchToggle(Z)Z
                        move-result v$freeRegisterC
                        if-nez v$freeRegisterC, :ignore
                        sget-object v$freeRegisterC, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                        if-eq v$freeRegisterC, v$freeRegisterE, :toggle_off
                        const-string v$freeRegisterE, "stable_volume_on"
                        invoke-static {v$freeRegisterE}, $PLAYER_CLASS_DESCRIPTOR->getToggleString(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$freeRegisterE
                        goto :set_string
                        :toggle_off
                        const-string v$freeRegisterE, "stable_volume_off"
                        invoke-static {v$freeRegisterE}, $PLAYER_CLASS_DESCRIPTOR->getToggleString(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$freeRegisterE
                        :set_string
                        iget-object v$freeRegisterC, v$classRegister, $iGetObjectPrimaryReference
                        check-cast v$freeRegisterC, $checkCastReference
                        iget-object v$freeRegisterC, v$freeRegisterC, $iGetObjectSecondaryReference
                        const-string v$freeRegisterD, "menu_item_cinematic_lighting"
                        invoke-virtual {v$freeRegisterC, v$freeRegisterD, v$freeRegisterE}, $stringReference
                        """, ExternalLabel("ignore", getInstruction(insertIndex))
                )
            }
        }

        fun changeToggleHook(
            fingerprint: Pair<String, Fingerprint>,
            methodToCall: String
        ) {
            val method = if (fingerprint == playbackLoopInitFingerprint)
                fingerprint.methodOrThrow(playbackLoopOnClickListenerFingerprint)
            else
                fingerprint.methodOrThrow()

            method.apply {
                val referenceIndex = indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            (this as ReferenceInstruction).reference.toString()
                                .endsWith(methodToCall)
                }
                if (referenceIndex > 0) {
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(referenceIndex + 1).registerA

                    addInstructions(
                        referenceIndex + 2, """
                            invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->changeSwitchToggle(Z)Z
                            move-result v$insertRegister
                            """
                    )
                } else {
                    if (fingerprint == cinematicLightingFingerprint)
                        changeToggleCinematicLightingHook()
                    else
                        throw PatchException("Target reference was not found in ${fingerprint.first}.")
                }
            }
        }


        val additionalSettingsConfigMethod =
            additionalSettingsConfigFingerprint.methodOrThrow()
        val methodToCall =
            additionalSettingsConfigMethod.definingClass + "->" + additionalSettingsConfigMethod.name + "()Z"

        var fingerprintArray = arrayOf(
            cinematicLightingFingerprint,
            playbackLoopInitFingerprint,
            playbackLoopOnClickListenerFingerprint,
            stableVolumeFingerprint
        )

        if (pipFingerprint.resolvable()) {
            fingerprintArray += pipFingerprint
        }

        fingerprintArray.forEach { fingerprint ->
            changeToggleHook(fingerprint, methodToCall)
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: FLYOUT_MENU",
                "SETTINGS: CHANGE_PLAYER_FLYOUT_MENU_TOGGLE"
            ),
            CHANGE_PLAYER_FLYOUT_MENU_TOGGLES
        )

        // endregion

    }
}
