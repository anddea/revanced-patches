package app.revanced.patches.music.layout.compactheader.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.music.layout.compactheader.fingerprints.CompactHeaderConstructorFingerprint
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x

@Patch
@DependsOn([MusicIntegrationsPatch::class, MusicSettingsPatch::class])
@Name("compact-header")
@Description("Hides the music category bar at the top of the homepage.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CompactHeaderPatch : BytecodePatch(
    listOf(
        CompactHeaderConstructorFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val result = CompactHeaderConstructorFingerprint.result!!
        val method = result.mutableMethod

        val insertIndex = result.scanResult.patternScanResult!!.endIndex
        val register = (method.implementation!!.instructions[insertIndex - 1] as BuilderInstruction11x).registerA
        method.addInstructions(
            insertIndex, """
                invoke-static {}, Lapp/revanced/integrations/settings/MusicSettings;->getCompactHeader()I
                move-result v2
                invoke-virtual {v${register}, v2}, Landroid/view/View;->setVisibility(I)V
            """
        )

        return PatchResultSuccess()
    }
}
