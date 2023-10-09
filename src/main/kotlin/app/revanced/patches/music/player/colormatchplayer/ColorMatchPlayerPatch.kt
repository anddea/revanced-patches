package app.revanced.patches.music.player.colormatchplayer

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.utils.fingerprints.PlayerColorFingerprint
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_PLAYER
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import kotlin.properties.Delegates

@Patch(
    name = "Enable color match player",
    description = "Matches the color of the mini player and the fullscreen player.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.15.52",
                "6.20.51",
                "6.21.51",
                "6.22.51"
            ]
        )
    ]
)
@Suppress("unused")
object ColorMatchPlayerPatch : BytecodePatch(
    setOf(PlayerColorFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PlayerColorFingerprint.result?.let {
            it.mutableMethod.apply {
                relativeIndex = it.scanResult.patternScanResult!!.startIndex
                val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.IPUT_OBJECT
                }
                val jumpInstruction = getInstruction<Instruction>(insertIndex)
                val replaceReference =
                    getInstruction<ReferenceInstruction>(insertIndex - 1).reference

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $MUSIC_PLAYER->enableColorMatchPlayer()Z
                        move-result v2
                        if-eqz v2, :off
                        iget v0, p0, ${descriptor(2)}
                        if-eq v0, v2, :switch
                        iput v2, p0, ${descriptor(2)}
                        iget-object v0, p0, ${descriptor(3)}
                        invoke-virtual {v0, v2, p2, p3}, ${descriptor(4)}
                        :switch
                        iget v0, p0, ${descriptor(7)}
                        if-eq v0, v1, :exit
                        iput v1, p0, ${descriptor(7)}
                        iget-object v0, p0, ${descriptor(8)}
                        invoke-virtual {v0, v1, p2, p3}, ${descriptor(9)}
                        goto :exit
                        :off
                        invoke-direct {p0}, $replaceReference
                        """, ExternalLabel("exit", jumpInstruction)
                )
                removeInstruction(insertIndex - 1)
            }
        } ?: throw PlayerColorFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_color_match_player",
            "true"
        )

    }

    private var relativeIndex by Delegates.notNull<Int>()

    private fun MutableMethod.descriptor(index: Int): String {
        return getInstruction<ReferenceInstruction>(relativeIndex + index).reference.toString()
    }
}