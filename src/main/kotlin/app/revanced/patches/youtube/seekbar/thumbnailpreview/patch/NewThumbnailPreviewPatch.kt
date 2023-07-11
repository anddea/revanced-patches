package app.revanced.patches.youtube.seekbar.thumbnailpreview.patch

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
import app.revanced.patches.youtube.seekbar.thumbnailpreview.fingerprints.ThumbnailPreviewConfigFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Enable new thumbnail preview")
@Description("Enables a new type of thumbnail preview.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class NewThumbnailPreviewPatch : BytecodePatch(
    listOf(ThumbnailPreviewConfigFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        ThumbnailPreviewConfigFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$targetRegister}, $SEEKBAR->enableNewThumbnailPreview(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: return ThumbnailPreviewConfigFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: ENABLE_NEW_THUMBNAIL_PREVIEW"
            )
        )

        SettingsPatch.updatePatchStatus("enable-new-thumbnail-preview")

        return PatchResultSuccess()
    }
}
