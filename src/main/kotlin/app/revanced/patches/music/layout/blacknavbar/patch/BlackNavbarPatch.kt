package app.revanced.patches.music.layout.blacknavbar.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.layout.blacknavbar.fingerprints.TabLayoutFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.util.integrations.Constants.MUSIC_SETTINGS_PATH
import org.jf.dexlib2.iface.instruction.formats.Instruction11x
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Patch
@Name("enable-black-navbar")
@Description("Sets the navigation bar color to black.")
@DependsOn(
    [
        MusicSettingsPatch::class,
        SharedResourcdIdPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class BlackNavbarPatch : BytecodePatch(
    listOf(
        TabLayoutFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val result = TabLayoutFingerprint.result!!
        val method = result.mutableMethod

        val startIndex = result.scanResult.patternScanResult!!.startIndex
        val endIndex = result.scanResult.patternScanResult!!.endIndex
        val insertIndex = endIndex + 1

        val dummyRegister = (method.instruction(startIndex) as Instruction31i).registerA
        val targetRegister = (method.instruction(endIndex) as Instruction11x).registerA

        method.addInstructions(
            insertIndex, """
                invoke-static {}, $MUSIC_SETTINGS_PATH->enableBlackNavbar()Z
                move-result v$dummyRegister
                if-eqz v$dummyRegister, :default
                const/high16 v$targetRegister, -0x1000000
            """, listOf(ExternalLabel("default", method.instruction(insertIndex)))
        )

        return PatchResultSuccess()
    }
}
