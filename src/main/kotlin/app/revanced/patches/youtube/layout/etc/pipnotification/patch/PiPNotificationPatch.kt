package app.revanced.patches.youtube.layout.etc.pipnotification.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.etc.pipnotification.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch

@Patch
@Name("hide-pip-notification")
@Description("Disable pip notification when you first launch pip mode.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class PiPNotificationPatch : BytecodePatch(
    listOf(
        PrimaryPiPFingerprint,
        SecondaryPiPFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            PrimaryPiPFingerprint,
            SecondaryPiPFingerprint
        ).map {
            it.result ?: return it.toErrorResult()
        }.forEach {
            val index = it.scanResult.patternScanResult!!.startIndex + 1
            it.mutableMethod.addInstruction(index, "return-void")
        }

        /*
         * Add settings
         */
        SettingsPatch.updatePatchStatus("hide-pip-notification")

        return PatchResultSuccess()
    }
}