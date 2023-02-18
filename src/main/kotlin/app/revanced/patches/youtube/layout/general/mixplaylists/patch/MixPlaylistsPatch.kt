package app.revanced.patches.youtube.layout.general.mixplaylists.patch

import app.revanced.extensions.injectHideCall
import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.mixplaylists.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction21c

@Patch
@Name("hide-mix-playlists")
@Description("Removes mix playlists from home feed and video player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class MixPlaylistsPatch : BytecodePatch(
    listOf(
        CreateMixPlaylistFingerprint,
        SecondCreateMixPlaylistFingerprint,
        ThirdCreateMixPlaylistFingerprint,
        FourthCreateMixPlaylistFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            CreateMixPlaylistFingerprint,
            SecondCreateMixPlaylistFingerprint
        ).forEach {
            it.result?.addHook()  ?: return it.toErrorResult()
        }

        arrayOf(
            ThirdCreateMixPlaylistFingerprint to true,
            FourthCreateMixPlaylistFingerprint to false
        ).map { (fingerprint, boolean) ->
            fingerprint.result?.hookMixPlaylists(boolean) ?: return fingerprint.toErrorResult()
        }

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: LAYOUT_SETTINGS",
                "PREFERENCE_HEADER: GENERAL",
                "SETTINGS: HIDE_MIX_PLAYLISTS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-mix-playlists")

        return PatchResultSuccess()
    }

    private fun MethodFingerprintResult.addHook() {
        val insertIndex = scanResult.patternScanResult!!.endIndex - 3
        val register = (mutableMethod.instruction(insertIndex - 2) as OneRegisterInstruction).registerA

        mutableMethod.implementation!!.injectHideCall(insertIndex, register, "layout/GeneralLayoutPatch", "hideMixPlaylists")
    }

    private fun MethodFingerprintResult.hookMixPlaylists(isThirdFingerprint: Boolean) {
        fun getRegister(instruction: Instruction): Int {
            if (isThirdFingerprint) return (instruction as TwoRegisterInstruction).registerA
            return (instruction as Instruction21c).registerA
        }
        val endIndex = scanResult.patternScanResult!!.endIndex
        val instruction = method.implementation!!.instructions.elementAt(endIndex)
        val register = getRegister(instruction)

        mutableMethod.implementation!!.injectHideCall(endIndex, register, "layout/GeneralLayoutPatch", "hideMixPlaylists")
    }
}
