package app.revanced.patches.music.layout.miniplayercolor.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.layout.miniplayercolor.fingerprints.MiniplayerColorFingerprint
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.fingerprints.MiniplayerColorParentFingerprint
import app.revanced.shared.util.integrations.Constants.MUSIC_SETTINGS_PATH
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@DependsOn([MusicIntegrationsPatch::class, MusicSettingsPatch::class])
@Name("enable-color-match-player")
@Description("Matches the fullscreen player color with the minimized one.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class MiniplayerColorPatch : BytecodePatch(
    listOf(
        MiniplayerColorFingerprint, MiniplayerColorParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        val miniplayerColorParentResult = MiniplayerColorParentFingerprint.result!!
        val miniplayerColorParentMethod = miniplayerColorParentResult.mutableMethod

        MiniplayerColorFingerprint.resolve(context, miniplayerColorParentResult.classDef)
        val miniplayerColorResult = MiniplayerColorFingerprint.result!!
        val miniplayerColorMethod = miniplayerColorResult.mutableMethod
        val insertIndex = miniplayerColorResult.scanResult.patternScanResult!!.startIndex + 1
        val jumpInstruction = miniplayerColorMethod.implementation!!.instructions[insertIndex] as Instruction

        val Reference_A_1 =
            miniplayerColorParentMethod.let { method ->
                (method.implementation!!.instructions.elementAt(4) as ReferenceInstruction).reference as FieldReference
            }
        val Reference_A_2 =
            miniplayerColorParentMethod.let { method ->
                (method.implementation!!.instructions.elementAt(5) as ReferenceInstruction).reference as FieldReference
            }
        val Reference_A_3 =
            miniplayerColorParentMethod.let { method ->
                (method.implementation!!.instructions.elementAt(6) as ReferenceInstruction).reference as MethodReference
            }
        val Reference_B_1 =
            miniplayerColorParentMethod.let { method ->
                (method.implementation!!.instructions.elementAt(10) as ReferenceInstruction).reference as FieldReference
            }
        val Reference_B_2 =
            miniplayerColorParentMethod.let { method ->
                (method.implementation!!.instructions.elementAt(11) as ReferenceInstruction).reference as FieldReference
            }
        val Reference_B_3 =
            miniplayerColorParentMethod.let { method ->
                (method.implementation!!.instructions.elementAt(12) as ReferenceInstruction).reference as MethodReference
            }

        miniplayerColorMethod.addInstructions(
            insertIndex, """
            invoke-static {}, $MUSIC_SETTINGS_PATH->enableColorMatchPlayer()Z
            move-result v2
            if-eqz v2, :off
            iget v0, p0, ${miniplayerColorResult.classDef.type}->${Reference_A_1.name}:${Reference_A_1.type}
            if-eq v0, v2, :abswitch
            iput v2, p0, ${miniplayerColorResult.classDef.type}->${Reference_A_1.name}:${Reference_A_1.type}
            iget-object v0, p0, ${miniplayerColorResult.classDef.type}->${Reference_A_2.name}:${Reference_A_2.type}
            invoke-virtual {v0, v2, p2, p3}, $Reference_A_3
            :abswitch
            iget v0, p0, ${miniplayerColorResult.classDef.type}->${Reference_B_1.name}:${Reference_B_1.type}
            if-eq v0, v1, :exit
            iput v1, p0, ${miniplayerColorResult.classDef.type}->${Reference_B_1.name}:${Reference_B_1.type}
            iget-object v0, p0, ${miniplayerColorResult.classDef.type}->${Reference_B_2.name}:${Reference_B_2.type}
            invoke-virtual {v0, v1, p2, p3}, $Reference_B_3
            goto :exit
            :off
            invoke-direct {p0}, ${miniplayerColorResult.classDef.type}->${miniplayerColorParentResult.mutableMethod.name}()V
        """, listOf(ExternalLabel("exit", jumpInstruction))
        )

        miniplayerColorMethod.removeInstruction(insertIndex - 1)

        return PatchResultSuccess()
    }
}