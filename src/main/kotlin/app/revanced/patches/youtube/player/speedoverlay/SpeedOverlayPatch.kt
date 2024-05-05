package app.revanced.patches.youtube.player.speedoverlay

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.RestoreSlideToSeekBehaviorFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

@Patch(
    name = "Disable speed overlay",
    description = "Adds an option to disable 'Play at 2x speed' when pressing and holding in the video player.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
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
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.43",
                "19.12.41",
                "19.13.37",
                "19.14.43",
                "19.15.36",
                "19.16.38"
            ]
        )
    ]
)
@Suppress("unused")
object SpeedOverlayPatch : BytecodePatch(
    setOf(
        RestoreSlideToSeekBehaviorFingerprint,
        SpeedOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        if (!SettingsPatch.upward1836)
            throw PatchException("This version is not supported. Please use YouTube 18.36.39 or later.")

        arrayOf(
            RestoreSlideToSeekBehaviorFingerprint,
            SpeedOverlayFingerprint
        ).forEach { fingerprint ->
            fingerprint.result?.let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex + 1
                    val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                                invoke-static {v$insertRegister}, $PLAYER->disableSpeedOverlay(Z)Z
                                move-result v$insertRegister
                                """
                    )
                }
            } ?: throw fingerprint.exception
        }

        if (SettingsPatch.upward1839) {
            SettingsPatch.contexts.document["res/layout/speedmaster_icon_edu_overlay.xml"].use { editor ->
                editor.doRecursively { node ->
                    arrayOf("height", "width").forEach replacement@{ replacement ->

                        if (node !is Element) return@replacement

                        if (node.attributes.getNamedItem("android:src")?.nodeValue?.endsWith("_24") != true)
                            return@replacement

                        node.getAttributeNode("android:layout_$replacement")?.let {
                            attribute -> attribute.textContent = "12.0dip"
                        }
                    }
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: DISABLE_SPEED_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("Disable speed overlay")
    }
}