package app.revanced.patches.youtube.layout.pipnotification.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.layout.pipnotification.fingerprints.PrimaryPiPFingerprint
import app.revanced.patches.youtube.layout.pipnotification.fingerprints.SecondaryPiPFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("Disable pip notification")
@Description("Disable pip notification when you first launch pip mode.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class PiPNotificationPatch : BytecodePatch(
    listOf(
        PrimaryPiPFingerprint,
        SecondaryPiPFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        arrayOf(
            PrimaryPiPFingerprint,
            SecondaryPiPFingerprint
        ).forEach { fingerprint ->
            fingerprint.result?.let {
                it.mutableMethod.apply {
                    addInstruction(
                        it.scanResult.patternScanResult!!.endIndex - 4,
                        "return-void"
                    )
                }
            } ?: throw fingerprint.exception
        }

        /**
         * Add settings
         */
        SettingsPatch.updatePatchStatus("hide-pip-notification")

    }
}