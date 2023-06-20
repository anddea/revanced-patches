package app.revanced.patches.youtube.general.mixplaylists.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.fingerprints.litho.EmptyComponentBuilderFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.general.mixplaylists.fingerprints.BottomPanelOverlayTextFingerprint
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-mix-playlists")
@Description("Removes mix playlists from home feed and video player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class MixPlaylistsPatch : BytecodePatch(
    listOf(
        BottomPanelOverlayTextFingerprint,
        EmptyComponentBuilderFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /**
         * Hide MixPlaylists when tablet UI is turned on
         * Required only for RVX Patches
         */
        BottomPanelOverlayTextFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $GENERAL->hideMixPlaylists(Landroid/view/View;)V"
                )
            }
        } ?: return BottomPanelOverlayTextFingerprint.toErrorResult()

        /**
         * Separated from bytebuffer patch
         * Target method is only used for Hide MixPlaylists patch
         */
        EmptyComponentBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val freeIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.CONST_4
                }
                val jumpIndex = getStringIndex("Failed to convert Element to Flatbuffers: %s") + 2
                val insertIndex = freeIndex + 5

                val freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA + 1

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {v$freeRegister}, $GENERAL->hideMixPlaylists([B)Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :not_an_ad
                    """, ExternalLabel("not_an_ad", getInstruction(jumpIndex))
                )

                addInstruction(
                    0,
                    "move-object/from16 v$freeRegister, p3"
                )
            }
        } ?: return EmptyComponentBuilderFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_MIX_PLAYLISTS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-mix-playlists")

        return PatchResultSuccess()
    }
}
