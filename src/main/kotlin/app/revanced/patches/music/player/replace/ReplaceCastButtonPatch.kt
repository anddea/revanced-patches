package app.revanced.patches.music.player.replace

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patches.music.player.replace.fingerprints.CastButtonContainerFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.music.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.music.utils.mainactivity.MainActivityResolvePatch.mainActivityMutableClass
import app.revanced.patches.music.utils.playerresponse.PlayerResponsePatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.PlayerCastMediaRouteButton
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.utils.settings.SettingsPatch.contexts
import app.revanced.patches.music.utils.videotype.VideoTypeHookPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object ReplaceCastButtonPatch : BaseBytecodePatch(
    name = "Replace cast button",
    description = "Adds an option to replace the cast button in the player with the \"Open music\" button.",
    dependencies = setOf(
        MainActivityResolvePatch::class,
        PlayerResponsePatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoTypeHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: BytecodeContext) {
        CastButtonContainerFingerprint.resolve(context, mainActivityMutableClass)

        CastButtonContainerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val freeIndex = getWideLiteralInstructionIndex(PlayerCastMediaRouteButton) + 1
                val freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

                val getActivityIndex = freeIndex - 4
                val getActivityRegister =
                    getInstruction<TwoRegisterInstruction>(getActivityIndex).registerB
                val getActivityReference =
                    getInstruction<ReferenceInstruction>(getActivityIndex).reference

                for (index in freeIndex + 20 downTo freeIndex) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_VIRTUAL)
                        continue

                    if ((getInstruction<ReferenceInstruction>(index).reference as MethodReference).name != "addView")
                        continue

                    val viewGroupInstruction = getInstruction<Instruction35c>(index)

                    addInstruction(
                        index + 1,
                        "invoke-static {v$freeRegister, v${viewGroupInstruction.registerC}, v${viewGroupInstruction.registerD}}, " +
                                PLAYER_CLASS_DESCRIPTOR +
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
        }

        PlayerResponsePatch.injectPlaylistCall(
            "$UTILS_PATH/CheckMusicVideoPatch;" +
                    "->" +
                    "playbackStart(Ljava/lang/String;Ljava/lang/String;IZ)V"
        )

        arrayOf(
            ResourceGroup(
                "layout",
                "open_music_button.xml"
            )
        ).forEach { resourceGroup ->
            contexts.copyResources("music/cast", resourceGroup)
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_replace_player_cast_button",
            "false"
        )

    }
}
