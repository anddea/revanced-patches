package app.revanced.patches.youtube.general.mixplaylists.patch

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
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.general.mixplaylists.fingerprints.BottomPanelOverlayTextFingerprint
import app.revanced.patches.youtube.general.mixplaylists.fingerprints.ElementParserFingerprint
import app.revanced.patches.youtube.general.mixplaylists.fingerprints.EmptyFlatBufferFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import app.revanced.util.integrations.Constants.GENERAL
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Hide mix playlists")
@Description("Hides mix playlists from home feed and video player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class MixPlaylistsPatch : BytecodePatch(
    listOf(
        BottomPanelOverlayTextFingerprint,
        ElementParserFingerprint,
        EmptyFlatBufferFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

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
        } ?: throw BottomPanelOverlayTextFingerprint.exception

        /**
         * Separated from bytebuffer patch
         * Target method is only used for Hide MixPlaylists patch
         */
        ElementParserFingerprint.result
            ?: EmptyFlatBufferFingerprint.result
            ?: throw EmptyFlatBufferFingerprint.exception


        /**
         * ~YouTube v18.29.38
         */
        EmptyFlatBufferFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.CHECK_CAST
                } + 1
                val jumpIndex = getStringIndex("Failed to convert Element to Flatbuffers: %s") + 2

                val freeIndex = it.scanResult.patternScanResult!!.startIndex - 1

                inject(freeIndex, insertIndex, jumpIndex)
            }
        }

        /**
         * YouTube v18.30.xx~
         */
        ElementParserFingerprint.result?.let {
            it.mutableMethod.apply {
                val methodInstructions = implementation!!.instructions

                val insertIndex = methodInstructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.INVOKE_INTERFACE
                }
                val freeIndex = it.scanResult.patternScanResult!!.startIndex - 1

                for (index in methodInstructions.size - 1 downTo 0) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_INTERFACE_RANGE) continue

                    val jumpIndex = index + 1

                    inject(freeIndex, insertIndex, jumpIndex)

                    break
                }
            }
        }

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

    }

    private companion object {
        fun MutableMethod.inject(
            freeIndex: Int,
            insertIndex: Int,
            jumpIndex: Int
        ) {
            val freeRegister = getInstruction<TwoRegisterInstruction>(freeIndex).registerA

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
    }
}
