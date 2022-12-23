package app.revanced.patches.music.layout.minimizedplayer.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.layout.minimizedplayer.fingerprints.MinimizedPlayerFingerprint
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@DependsOn([MusicIntegrationsPatch::class, MusicSettingsPatch::class])
@Name("minimized-player")
@Description("Permanently keep player minimized even if another track is played.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class MinimizedPlayerPatch : BytecodePatch(
    listOf(
        MinimizedPlayerFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        val result = MinimizedPlayerFingerprint.result!!
        val method = result.mutableMethod
        val index = result.scanResult.patternScanResult!!.endIndex
        val register = (method.implementation!!.instructions[index - 1] as OneRegisterInstruction).registerA - 1
        val jumpInstruction = method.implementation!!.instructions[index + 1] as Instruction

        method.addInstructions(
            index, """
            invoke-static {}, Lapp/revanced/integrations/settings/MusicSettings;->getEnforceMinimizedPlayer()Z
            move-result v$register
            if-nez v$register, :enforce
        """, listOf(ExternalLabel("enforce", jumpInstruction))
        )

        return PatchResultSuccess()
    }
}