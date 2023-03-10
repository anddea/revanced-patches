package app.revanced.patches.music.layout.blacknavbar.patch

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
import app.revanced.patches.music.layout.blacknavbar.fingerprints.TabLayoutFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.integrations.Constants.MUSIC_SETTINGS_PATH
import org.jf.dexlib2.iface.instruction.formats.Instruction11x
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Patch
@Name("enable-black-navbar")
@Description("Sets the navigation bar color to black.")
@DependsOn(
    [
        MusicSettingsPatch::class,
        SharedResourceIdPatch::class
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

        TabLayoutFingerprint.result?.let {
            with (it.mutableMethod) {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val insertIndex = endIndex + 1

                val dummyRegister = (instruction(startIndex) as Instruction31i).registerA
                val targetRegister = (instruction(endIndex) as Instruction11x).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {}, $MUSIC_SETTINGS_PATH->enableBlackNavbar()Z
                        move-result v$dummyRegister
                        if-eqz v$dummyRegister, :default
                        const/high16 v$targetRegister, -0x1000000
                        """, listOf(ExternalLabel("default", instruction(insertIndex)))
                )
            }
        } ?: return TabLayoutFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference("design", "revanced_enable_black_navbar", "true")

        return PatchResultSuccess()
    }
}
