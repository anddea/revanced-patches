package app.revanced.patches.youtube.layout.general.mixplaylists.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.layout.general.mixplaylists.bytecode.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.injectHideCall
import app.revanced.shared.util.bytecode.BytecodeHelper
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Name("hide-mix-playlists-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class MixPlaylistsBytecodePatch : BytecodePatch(
    listOf(
        CreateMixPlaylistFingerprint,
        SecondCreateMixPlaylistFingerprint,
        ThirdCreateMixPlaylistFingerprint,
        FourthCreateMixPlaylistFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(CreateMixPlaylistFingerprint, SecondCreateMixPlaylistFingerprint).forEach(::addHook)
        ThirdCreateMixPlaylistFingerprint.hookMixPlaylists(true)
        FourthCreateMixPlaylistFingerprint.hookMixPlaylists(false)

        return PatchResultSuccess()
    }

    private fun addHook(fingerprint: MethodFingerprint) {
        with (fingerprint.result!!) {
            val insertIndex = scanResult.patternScanResult!!.endIndex - 3

            val register = (mutableMethod.instruction(insertIndex - 2) as OneRegisterInstruction).registerA

            mutableMethod.implementation!!.injectHideCall(insertIndex, register, "layout/GeneralLayoutPatch", "hideMixPlaylists")
        }
    }

    fun MethodFingerprint.hookMixPlaylists(isThirdFingerprint: Boolean) {
        fun getRegister(instruction: Instruction): Int {
                if (isThirdFingerprint) return (instruction as TwoRegisterInstruction).registerA
                return (instruction as Instruction21c).registerA
        }
        with(this.result!!) {
                val endIndex = scanResult.patternScanResult!!.endIndex
                val instruction = method.implementation!!.instructions.elementAt(endIndex)
                val register = getRegister(instruction)

                mutableMethod.implementation!!.injectHideCall(endIndex, register, "layout/GeneralLayoutPatch", "hideMixPlaylists")
        }
    }
}
