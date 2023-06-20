package app.revanced.patches.music.layout.dismissqueue.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.layout.dismissqueue.fingerprints.DismissQueueFingerprint
import app.revanced.patches.music.utils.settings.resource.patch.MusicSettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-dismiss-queue")
@Description("Add dismiss queue to flyout menu. (YT Music v6.04.51+)")
@DependsOn([MusicSettingsPatch::class])
@MusicCompatibility
@Version("0.0.1")
class DismissQueuePatch : BytecodePatch(
    listOf(DismissQueueFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        DismissQueueFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$targetRegister}, $MUSIC_LAYOUT->enableDismissQueue(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: return DismissQueueFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(
            CategoryType.LAYOUT,
            "revanced_enable_dismiss_queue",
            "true"
        )

        return PatchResultSuccess()
    }
}