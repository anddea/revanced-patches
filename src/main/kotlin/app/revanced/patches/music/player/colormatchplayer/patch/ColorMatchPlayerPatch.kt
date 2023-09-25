package app.revanced.patches.music.player.colormatchplayer.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.fingerprints.PlayerColorFingerprint
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_PLAYER
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import kotlin.properties.Delegates

@Patch
@Name("Enable color match player")
@Description("Matches the color of the mini player and the fullscreen player.")
@DependsOn([SettingsPatch::class])
@MusicCompatibility
class ColorMatchPlayerPatch : BytecodePatch(
    listOf(PlayerColorFingerprint)
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

    private companion object {
        var relativeIndex by Delegates.notNull<Int>()

        fun MutableMethod.descriptor(index: Int): String {
            return getInstruction<ReferenceInstruction>(relativeIndex + index).reference.toString()
        }
    }
}