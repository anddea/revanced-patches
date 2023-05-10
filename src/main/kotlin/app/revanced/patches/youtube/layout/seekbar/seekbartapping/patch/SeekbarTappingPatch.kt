package app.revanced.patches.youtube.layout.seekbar.seekbartapping.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.seekbar.seekbartapping.fingerprints.SeekbarTappingFingerprint
import app.revanced.patches.youtube.layout.seekbar.seekbartapping.fingerprints.SeekbarTappingParentFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.formats.Instruction11n
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("enable-seekbar-tapping")
@Description("Enables tap-to-seek on the seekbar of the video player.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SeekbarTappingPatch : BytecodePatch(
    listOf(
        SeekbarTappingParentFingerprint,
        SeekbarTappingFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val tapSeekMethods = mutableMapOf<String, Method>()

        SeekbarTappingParentFingerprint.result?.let { parentResult ->
            for (it in parentResult.classDef.methods) {
                if (it.implementation == null) continue

                val instructions = it.implementation!!.instructions
                // here we make sure we actually find the method because it has more than 7 instructions
                if (instructions.count() < 7) continue

                // we know that the 7th instruction has the opcode CONST_4
                val instruction = instructions.elementAt(6)
                if (instruction.opcode != Opcode.CONST_4) continue

                // the literal for this instruction has to be either 1 or 2
                val literal = (instruction as Instruction11n).narrowLiteral

                // method founds
                if (literal == 1) tapSeekMethods["P"] = it
                if (literal == 2) tapSeekMethods["O"] = it
            }
        } ?: return SeekbarTappingParentFingerprint.toErrorResult()

        SeekbarTappingFingerprint.result?.let {
            val insertIndex = it.scanResult.patternScanResult!!.endIndex

            it.mutableMethod.apply {
                val register = instruction<Instruction35c>(insertIndex - 1).registerC

                val pMethod = tapSeekMethods["P"]!!
                val oMethod = tapSeekMethods["O"]!!

                addInstructions(
                    insertIndex, """
                        invoke-static {}, $SEEKBAR->enableSeekbarTapping()Z
                        move-result v0
                        if-eqz v0, :off
                        invoke-virtual { v$register, v2 }, ${oMethod.definingClass}->${oMethod.name}(I)V
                        invoke-virtual { v$register, v2 }, ${pMethod.definingClass}->${pMethod.name}(I)V
                        """, listOf(ExternalLabel("off", instruction(insertIndex)))
                )
            }
        } ?: return SeekbarTappingFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: ENABLE_SEEKBAR_TAPPING"
            )
        )

        SettingsPatch.updatePatchStatus("enable-seekbar-tapping")

        return PatchResultSuccess()
    }
}