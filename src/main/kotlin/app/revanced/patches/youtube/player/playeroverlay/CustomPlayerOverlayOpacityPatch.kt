package app.revanced.patches.youtube.player.playeroverlay

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ScrimOverlay
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.PLAYER
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch(
    name = "Custom player overlay opacity",
    description = "Change the opacity of the player background, when player controls are visible.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43"
            ]
        )
    ]
)
@Suppress("unused")
object CustomPlayerOverlayOpacityPatch : BytecodePatch(
    setOf(YouTubeControlsOverlayFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        YouTubeControlsOverlayFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(ScrimOverlay) + 3
                val targetParameter = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                if (!targetParameter.toString().endsWith("Landroid/widget/ImageView;"))
                    throw PatchException("Method signature parameter did not match: $targetParameter")

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $PLAYER->changePlayerOpacity(Landroid/widget/ImageView;)V"
                )
            }
        } ?: throw YouTubeControlsOverlayFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: CUSTOM_PLAYER_OVERLAY_OPACITY"
            )
        )

        SettingsPatch.updatePatchStatus("Custom player overlay opacity")

    }
}