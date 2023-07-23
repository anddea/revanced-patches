package app.revanced.patches.youtube.player.suggestedvideooverlay.patch

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
import app.revanced.patches.youtube.overlaybutton.general.patch.OverlayButtonsPatch
import app.revanced.patches.youtube.player.suggestedvideooverlay.fingerprints.CoreConatinerBuilderFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.CoreContainer
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.PLAYER
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Hide suggested video overlay")
@Description("Hide the suggested video overlay to play next.")
@DependsOn(
    [
        OverlayButtonsPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SuggestedVideoOverlayPatch : BytecodePatch(
    listOf(CoreConatinerBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        CoreConatinerBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(CoreContainer) + 4
                val targetReference =
                    getInstruction<ReferenceInstruction>(targetIndex).reference

                if (!targetReference.toString().endsWith("Landroid/view/ViewGroup;"))
                    return PatchResultError("Reference did not match: $targetReference")

                val targetRegister =
                    getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR"
                )
            }
        } ?: return CoreConatinerBuilderFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: PLAYER_EXPERIMENTAL_FLAGS",
                "SETTINGS: HIDE_SUGGESTED_VIDEO_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("hide-suggested-video-overlay")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$PLAYER->hideSuggestedVideoOverlay(Landroid/view/ViewGroup;)V"
    }
}