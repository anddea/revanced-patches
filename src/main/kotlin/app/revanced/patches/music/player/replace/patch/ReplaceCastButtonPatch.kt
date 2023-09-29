package app.revanced.patches.music.player.replace.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.player.replace.fingerprints.CastButtonContainerFingerprint
import app.revanced.patches.music.player.replace.fingerprints.PlaybackStartDescriptorFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.PlayerCastMediaRouteButton
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch.Companion.contexts
import app.revanced.patches.music.utils.videotype.patch.VideoTypeHookPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_PLAYER
import app.revanced.util.integrations.Constants.MUSIC_UTILS_PATH
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(false)
@Name("Replace cast button")
@Description("Replace the cast button in the player with the open music button.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoTypeHookPatch::class
    ]
)
@MusicCompatibility
class ReplaceCastButtonPatch : BytecodePatch(
    listOf(
        CastButtonContainerFingerprint,
        PlaybackStartDescriptorFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        CastButtonContainerFingerprint.result?.let {
            it.mutableMethod.apply {
                val freeIndex = getWideLiteralIndex(PlayerCastMediaRouteButton) + 1
                val freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

                val getActivityIndex = freeIndex - 4
                val getActivityRegister = getInstruction<TwoRegisterInstruction>(getActivityIndex).registerB
                val getActivityReference = getInstruction<ReferenceInstruction>(getActivityIndex).reference

                for (index in freeIndex + 20 downTo freeIndex) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_VIRTUAL)
                        continue

                    if ((getInstruction<ReferenceInstruction>(index).reference as MethodReference).name != "addView")
                        continue

                    val viewGroupInstruction = getInstruction<Instruction35c>(index)

                    addInstruction(
                        index + 1,
                        "invoke-static {v$freeRegister, v${viewGroupInstruction.registerC}, v${viewGroupInstruction.registerD}}, " +
                                MUSIC_PLAYER +
                                "->" +
                                "replaceCastButton(Landroid/app/Activity;Landroid/view/ViewGroup;Landroid/view/View;)V"
                    )
                    addInstruction(
                        index + 1,
                        "iget-object v$freeRegister, v$getActivityRegister, $getActivityReference"
                    )
                    removeInstruction(index)

                    break
                }
            }
        } ?: throw CastButtonContainerFingerprint.exception

        PlaybackStartDescriptorFingerprint.result?.let {
            it.mutableMethod.apply {
                val videoIdRegister = 1
                val playlistIdRegister = 4
                val playlistIndexRegister = 5

                addInstruction(
                    0,
                    "invoke-static {p$videoIdRegister, p$playlistIdRegister, p$playlistIndexRegister}, " +
                            "$MUSIC_UTILS_PATH/CheckMusicVideoPatch;" +
                            "->" +
                            "playbackStart(Ljava/lang/String;Ljava/lang/String;I)V"
                )
            }
        } ?: throw PlaybackStartDescriptorFingerprint.exception

        arrayOf(
            ResourceUtils.ResourceGroup(
                "layout",
                "open_music_button.xml"
            )
        ).forEach { resourceGroup ->
            contexts.copyResources("music/cast", resourceGroup)
        }

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_replace_player_cast_button",
            "false"
        )

    }
}
