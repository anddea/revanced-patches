package app.revanced.patches.youtube.player.suggestedvideooverlay

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.player.suggestedvideooverlay.fingerprints.CoreConatinerBuilderFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.CoreContainer
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.videoid.general.VideoIdPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.PLAYER
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(
    name = "Hide suggested video overlay",
    description = "Hide the suggested video overlay to play next.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.22.37",
                "18.23.36",
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40"
            ]
        )
    ]
)
@Suppress("unused")
object SuggestedVideoOverlayPatch : BytecodePatch(
    setOf(CoreConatinerBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        CoreConatinerBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(CoreContainer) + 4
                val targetReference =
                    getInstruction<ReferenceInstruction>(targetIndex).reference

                if (!targetReference.toString().endsWith("Landroid/view/ViewGroup;"))
                    throw PatchException("Reference did not match: $targetReference")

                val targetRegister =
                    getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $PLAYER->hideSuggestedVideoOverlay(Landroid/view/ViewGroup;)V"
                )
            }
        } ?: throw CoreConatinerBuilderFingerprint.exception

        VideoIdPatch.videoEndMethod.apply {
            addInstruction(
                implementation!!.instructions.size - 1,
                "invoke-static {},$PLAYER->hideSuggestedVideoOverlay()V"
            )
        }

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

    }

    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$PLAYER->hideSuggestedVideoOverlay(Landroid/view/ViewGroup;)V"
}