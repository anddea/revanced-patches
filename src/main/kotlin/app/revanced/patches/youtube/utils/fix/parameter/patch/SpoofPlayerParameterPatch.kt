package app.revanced.patches.youtube.utils.fix.parameter.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.ProtobufParameterBuilderFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.ScrubbedPreviewLayoutFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardThumbnailFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardThumbnailParentFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.SubtitleWindowFingerprint
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.videoid.general.patch.VideoIdPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("Spoof player parameters")
@Description("Spoofs player parameters to prevent playback issues.")
@DependsOn(
    [
        SharedResourceIdPatch::class,
        PlayerTypeHookPatch::class,
        VideoIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SpoofPlayerParameterPatch : BytecodePatch(
    listOf(
        ProtobufParameterBuilderFingerprint,
        ScrubbedPreviewLayoutFingerprint,
        StoryboardThumbnailParentFingerprint,
        SubtitleWindowFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        // hook parameter
        ProtobufParameterBuilderFingerprint.result?.let {
            (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                .getMethod() as MutableMethod
                    ).apply {
                    val protobufParam = 3

                    addInstructions(
                        0,
                        """
                        invoke-static {p$protobufParam}, $INTEGRATIONS_CLASS_DESCRIPTOR->overridePlayerParameter(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p$protobufParam
                    """
                    )
                }
        } ?: return ProtobufParameterBuilderFingerprint.toErrorResult()

        // When the player parameter is spoofed in incognito mode, this value will always be false
        // If this value is true, the timestamp and chapter are showned when tapping the seekbar.
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
                            invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->getSeekbarThumbnailOverrideValue(Z)Z
                            move-result v$targetRegister
                            return v$targetRegister
                            """
                    )
                    removeInstruction(targetIndex)
                }
            } ?: return StoryboardThumbnailFingerprint.toErrorResult()
        } ?: return StoryboardThumbnailParentFingerprint.toErrorResult()

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
        } ?: return ScrubbedPreviewLayoutFingerprint.toErrorResult()

        // fix protobuf spoof side issue
        SubtitleWindowFingerprint.result?.mutableMethod?.addInstructions(
            0,
            """
                invoke-static {p1, p2, p3, p4, p5}, $INTEGRATIONS_CLASS_DESCRIPTOR->getSubtitleWindowSettingsOverride(IIIZZ)[I
                move-result-object v0
                const/4 v1, 0x0
                aget p1, v0, v1     # ap, anchor position
                const/4 v1, 0x1
                aget p2, v0, v1     # ah, horizontal anchor
                const/4 v1, 0x2
                aget p3, v0, v1     # av, vertical anchor
            """
        ) ?: return SubtitleWindowFingerprint.toErrorResult()

        // Hook video id, required for subtitle fix.
        VideoIdPatch.injectCall("$MISC_PATH/SpoofPlayerParameterPatch;->setCurrentVideoId(Ljava/lang/String;)V")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: SPOOF_PLAYER_PARAMETER"
            )
        )

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$MISC_PATH/SpoofPlayerParameterPatch;"
    }
}
