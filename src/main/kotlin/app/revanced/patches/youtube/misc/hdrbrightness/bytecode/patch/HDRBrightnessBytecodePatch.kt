package app.revanced.patches.youtube.misc.hdrbrightness.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.misc.hdrbrightness.bytecode.fingerprints.HDRBrightnessFingerprint
import app.revanced.patches.youtube.misc.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference

@Name("enable-hdr-auto-brightness-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.2")
@DependsOn([LegacyVideoIdPatch::class])
class HDRBrightnessBytecodePatch : BytecodePatch(
    listOf(HDRBrightnessFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val result = HDRBrightnessFingerprint.result!!
        val method = result.mutableMethod

        method.implementation!!.instructions.filter { instruction ->
            val fieldReference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            fieldReference?.let { it.name == "screenBrightness" } == true
        }.forEach { instruction ->
            val brightnessRegisterIndex = method.implementation!!.instructions.indexOf(instruction)
            val register = (instruction as TwoRegisterInstruction).registerA

            val insertIndex = brightnessRegisterIndex + 1
            method.addInstructions(
                insertIndex,
                """
                   invoke-static {v$register}, $MISC_PATH/HDRAutoBrightnessPatch;->getHDRBrightness(F)F
                   move-result v$register
                """
            )
        }

        LegacyVideoIdPatch.injectCall("$MISC_PATH/HDRAutoBrightnessPatch;->newVideoStarted(Ljava/lang/String;)V")

        return PatchResultSuccess()
    }
}
