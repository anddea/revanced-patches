package app.revanced.patches.youtube.utils.fix.parameter.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.ProtobufParameterBuilderFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.ScrubbedPreviewLayoutFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardThumbnailFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardThumbnailParentFingerprint
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("Spoof player parameters")
@Description("Spoofs player parameters to prevent playback issues.")
@DependsOn(
    [
        SharedResourceIdPatch::class,
        PlayerTypeHookPatch::class
    ]
)
@YouTubeCompatibility
class SpoofPlayerParameterPatch : BytecodePatch(
    listOf(
        ProtobufParameterBuilderFingerprint,
        ScrubbedPreviewLayoutFingerprint,
        StoryboardThumbnailParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        // hook parameter
        ProtobufParameterBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val protobufParam = 3

                addInstructions(
                    0, """
                        invoke-static {p$protobufParam}, $INTEGRATIONS_CLASS_DESCRIPTOR->overridePlayerParameter(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p$protobufParam
                        """
                )
            }
        } ?: throw ProtobufParameterBuilderFingerprint.exception

        // When the player parameter is spoofed in incognito mode, this value will always be false
        // If this value is true, the timestamp and chapter are shown when tapping the seekbar.
        StoryboardThumbnailParentFingerprint.result?.classDef?.let { classDef ->
            StoryboardThumbnailFingerprint.also {
                it.resolve(
                    context,
                    classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.endIndex
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    // Since this is end of the method must replace one line then add the rest.
                    addInstructions(
                        targetIndex + 1,
                        """
                            invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->getSeekbarThumbnailOverrideValue()Z
                            move-result v$targetRegister
                            return v$targetRegister
                            """
                    )
                    removeInstruction(targetIndex)
                }
            } ?: throw StoryboardThumbnailFingerprint.exception
        } ?: throw StoryboardThumbnailParentFingerprint.exception

        // Seekbar thumbnail now show up but are always a blank image.
        // Additional changes are needed to force the client to generate the thumbnails (assuming it's possible),
        // but for now hide the empty thumbnail.
        ScrubbedPreviewLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val imageViewFieldName = getInstruction<ReferenceInstruction>(endIndex).reference

                addInstructions(
                    implementation!!.instructions.lastIndex,
                    """
                        iget-object v0, p0, $imageViewFieldName   # copy imageview field to a register
                        invoke-static {v0}, $INTEGRATIONS_CLASS_DESCRIPTOR->seekbarImageViewCreated(Landroid/widget/ImageView;)V
                        """
                )
            }
        } ?: throw ScrubbedPreviewLayoutFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: SPOOF_PLAYER_PARAMETER"
            )
        )

    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$MISC_PATH/SpoofPlayerParameterPatch;"
    }
}
