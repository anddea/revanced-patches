package app.revanced.patches.youtube.layout.player.playeroverlayfilter.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.player.playeroverlayfilter.fingerprints.ScrimOverlayFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.scrimOverlayId
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.PLAYER
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("hide-player-overlay-filter")
@Description("Remove the dark filter layer from the player's background.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class PlayerOverlayFilterPatch : BytecodePatch(
    listOf(ScrimOverlayFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        ScrimOverlayFingerprint.result?.mutableMethod?.let {
            val targetIndex = it.getWideLiteralIndex(scrimOverlayId) + 3
            val targetParameter = it.getInstruction<ReferenceInstruction>(targetIndex).reference
            val targetRegister = it.getInstruction<OneRegisterInstruction>(targetIndex).registerA

            if (!targetParameter.toString().endsWith("Landroid/widget/ImageView;"))
                return PatchResultError("Method signature parameter did not match: $targetParameter")

            it.addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $PLAYER->hidePlayerOverlayFilter(Landroid/widget/ImageView;)V"
            )
        } ?: return ScrimOverlayFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_PLAYER_OVERLAY_FILTER"
            )
        )

        SettingsPatch.updatePatchStatus("hide-player-overlay-filter")

        return PatchResultSuccess()
    }
}