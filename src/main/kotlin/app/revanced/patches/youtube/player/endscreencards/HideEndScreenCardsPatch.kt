package app.revanced.patches.youtube.player.endscreencards

import app.revanced.extensions.exception
import app.revanced.extensions.injectHideCall
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.player.endscreencards.fingerprints.LayoutCircleFingerprint
import app.revanced.patches.youtube.player.endscreencards.fingerprints.LayoutIconFingerprint
import app.revanced.patches.youtube.player.endscreencards.fingerprints.LayoutVideoFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide end screen cards",
    description = "Hides the suggested video cards at the end of a video in fullscreen.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class,
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
                "18.45.43",
                "18.46.43"
            ]
        )
    ]
)
@Suppress("unused")
object HideEndScreenCardsPatch : BytecodePatch(
    setOf(
        LayoutCircleFingerprint,
        LayoutIconFingerprint,
        LayoutVideoFingerprint,
    )
) {
    override fun execute(context: BytecodeContext) {

        fun MethodFingerprintResult.injectHideCalls() {
            val index = scanResult.patternScanResult!!.endIndex
            mutableMethod.apply {
                val register = this.getInstruction<OneRegisterInstruction>(index).registerA
                implementation!!.injectHideCall(
                    index + 1,
                    register,
                    "layout/PlayerPatch",
                    "hideEndScreenCards"
                )
            }
        }

        listOf(
            LayoutCircleFingerprint,
            LayoutIconFingerprint,
            LayoutVideoFingerprint
        ).forEach {
            it.result?.injectHideCalls() ?: throw it.exception
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_END_SCREEN_CARDS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide end screen cards")

    }
}
