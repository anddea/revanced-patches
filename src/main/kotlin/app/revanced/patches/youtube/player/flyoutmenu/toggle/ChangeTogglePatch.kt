package app.revanced.patches.youtube.player.flyoutmenu.toggle

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.flyoutmenu.toggle.fingerprints.AdditionalSettingsConfigFingerprint
import app.revanced.patches.youtube.player.flyoutmenu.toggle.fingerprints.CinematicLightingFingerprint
import app.revanced.patches.youtube.player.flyoutmenu.toggle.fingerprints.PiPFingerprint
import app.revanced.patches.youtube.player.flyoutmenu.toggle.fingerprints.PlaybackLoopInitFingerprint
import app.revanced.patches.youtube.player.flyoutmenu.toggle.fingerprints.PlaybackLoopOnClickListenerFingerprint
import app.revanced.patches.youtube.player.flyoutmenu.toggle.fingerprints.StableVolumeFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
object ChangeTogglePatch : BaseBytecodePatch(
    name = "Change player flyout menu toggles",
    description = "Adds an option to use text toggles instead of switch toggles within the additional settings menu.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AdditionalSettingsConfigFingerprint,
        CinematicLightingFingerprint,
        PiPFingerprint,
        PlaybackLoopOnClickListenerFingerprint,
        StableVolumeFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        val additionalSettingsConfigMethod = AdditionalSettingsConfigFingerprint.resultOrThrow().mutableMethod
        val methodToCall = additionalSettingsConfigMethod.definingClass + "->" + additionalSettingsConfigMethod.name + "()Z"

        // Resolves fingerprints
        val playbackLoopOnClickListenerResult = PlaybackLoopOnClickListenerFingerprint.resultOrThrow()
        PlaybackLoopInitFingerprint.resolve(context, playbackLoopOnClickListenerResult.classDef)

        var fingerprintArray = arrayOf(
            CinematicLightingFingerprint,
            PlaybackLoopInitFingerprint,
            PlaybackLoopOnClickListenerFingerprint,
            StableVolumeFingerprint
        )

        PiPFingerprint.result?.let {
            fingerprintArray += PiPFingerprint
        }

        fingerprintArray.forEach { fingerprint ->
            injectCall(fingerprint, methodToCall)
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: FLYOUT_MENU",
                "SETTINGS: CHANGE_PLAYER_FLYOUT_MENU_TOGGLE"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }

    private fun injectCall(
        fingerprint: MethodFingerprint,
        methodToCall: String
    ) {
        fingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val referenceIndex = indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_VIRTUAL
                            && (this as ReferenceInstruction).reference.toString().endsWith(methodToCall)
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
                    if (fingerprint == CinematicLightingFingerprint)
                        injectCinematicLightingMethod()
                    else
                        throw PatchException("Target reference was not found in ${fingerprint.javaClass.simpleName}.")
                }
            }
        }
    }

    private fun injectCinematicLightingMethod() {
        val stableVolumeMethod = StableVolumeFingerprint.resultOrThrow().mutableMethod

        val stringReferenceIndex = stableVolumeMethod.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL
                    && (this as ReferenceInstruction).reference.toString().endsWith("(Ljava/lang/String;Ljava/lang/String;)V")
        }
        if (stringReferenceIndex < 0)
            throw PatchException("Target reference was not found in ${StableVolumeFingerprint.javaClass.simpleName}.")

        val stringReference = stableVolumeMethod.getInstruction<ReferenceInstruction>(stringReferenceIndex).reference

        CinematicLightingFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val stringIndex = getStringInstructionIndex("menu_item_cinematic_lighting")

                val checkCastIndex = getTargetIndexReversed(stringIndex, Opcode.CHECK_CAST)
                val iGetObjectPrimaryIndex = getTargetIndexReversed(checkCastIndex, Opcode.IGET_OBJECT)
                val iGetObjectSecondaryIndex = getTargetIndex(checkCastIndex, Opcode.IGET_OBJECT)

                val checkCastReference = getInstruction<ReferenceInstruction>(checkCastIndex).reference
                val iGetObjectPrimaryReference = getInstruction<ReferenceInstruction>(iGetObjectPrimaryIndex).reference
                val iGetObjectSecondaryReference = getInstruction<ReferenceInstruction>(iGetObjectSecondaryIndex).reference

                val invokeVirtualIndex = getTargetIndex(stringIndex, Opcode.INVOKE_VIRTUAL)
                val invokeVirtualInstruction = getInstruction<FiveRegisterInstruction>(invokeVirtualIndex)
                val freeRegisterC = invokeVirtualInstruction.registerC
                val freeRegisterD = invokeVirtualInstruction.registerD
                val freeRegisterE = invokeVirtualInstruction.registerE

                val insertIndex = getTargetIndex(stringIndex, Opcode.RETURN_VOID)

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
                        iget-object v$freeRegisterC, p0, $iGetObjectPrimaryReference
                        check-cast v$freeRegisterC, $checkCastReference
                        iget-object v$freeRegisterC, v$freeRegisterC, $iGetObjectSecondaryReference
                        const-string v$freeRegisterD, "menu_item_cinematic_lighting"
                        invoke-virtual {v$freeRegisterC, v$freeRegisterD, v$freeRegisterE}, $stringReference
                        """, ExternalLabel("ignore", getInstruction(insertIndex))
                )
            }
        }
    }
}
