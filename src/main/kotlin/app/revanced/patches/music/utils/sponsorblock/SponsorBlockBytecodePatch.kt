package app.revanced.patches.music.utils.sponsorblock

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.sponsorblock.fingerprints.MusicPlaybackControlsTimeBarDrawFingerprint
import app.revanced.patches.music.utils.sponsorblock.fingerprints.MusicPlaybackControlsTimeBarOnMeasureFingerprint
import app.revanced.patches.music.utils.sponsorblock.fingerprints.RectangleFieldInvalidatorFingerprint
import app.revanced.patches.music.utils.sponsorblock.fingerprints.SeekBarConstructorFingerprint
import app.revanced.patches.music.utils.sponsorblock.fingerprints.SeekbarOnDrawFingerprint
import app.revanced.patches.music.video.information.VideoInformationPatch
import app.revanced.util.alsoResolve
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    dependencies = [
        SharedResourceIdPatch::class,
        VideoInformationPatch::class
    ]
)
object SponsorBlockBytecodePatch : BytecodePatch(
    setOf(
        MusicPlaybackControlsTimeBarDrawFingerprint,
        MusicPlaybackControlsTimeBarOnMeasureFingerprint,
        SeekBarConstructorFingerprint
    )
) {
    private const val INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/sponsorblock/SegmentPlaybackController;"

    override fun execute(context: BytecodeContext) {

        /**
         * Hook the video time methods & Initialize the player controller
         */
        VideoInformationPatch.apply {
            videoTimeHook(
                INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
                "setVideoTime"
            )
        }

        /**
         * Responsible for seekbar in fullscreen
         */
        var rectangleFieldName =
            RectangleFieldInvalidatorFingerprint.alsoResolve(
                context, SeekBarConstructorFingerprint
            ).let {
                with(it.mutableMethod) {
                    val invalidateIndex =
                        RectangleFieldInvalidatorFingerprint.indexOfInvalidateInstruction(this)
                    val rectangleIndex =
                        indexOfFirstInstructionReversedOrThrow(invalidateIndex + 1) {
                            getReference<FieldReference>()?.type == "Landroid/graphics/Rect;"
                        }
                    val rectangleReference =
                        getInstruction<ReferenceInstruction>(rectangleIndex).reference

                    (rectangleReference as FieldReference).name
                }
            }

        SeekbarOnDrawFingerprint.alsoResolve(
            context, SeekBarConstructorFingerprint
        ).let {
            it.mutableMethod.apply {
                // Initialize seekbar method
                addInstructions(
                    0, """
                        move-object/from16 v0, p0
                        const-string v1, "$rectangleFieldName"
                        invoke-static {v0, v1}, $INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;Ljava/lang/String;)V
                        """
                )

                // Set seekbar thickness
                val roundIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "round"
                } + 1
                val roundRegister = getInstruction<OneRegisterInstruction>(roundIndex).registerA
                addInstruction(
                    roundIndex + 1,
                    "invoke-static {v$roundRegister}, " +
                            "$INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
                )

                // Draw segment
                val drawCircleIndex = indexOfFirstInstructionReversedOrThrow {
                    getReference<MethodReference>()?.name == "drawCircle"
                }
                val drawCircleInstruction = getInstruction<FiveRegisterInstruction>(drawCircleIndex)
                addInstruction(
                    drawCircleIndex,
                    "invoke-static {v${drawCircleInstruction.registerC}, v${drawCircleInstruction.registerE}}, " +
                            "$INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
                )
            }
        }


        /**
         * Responsible for seekbar in player
         */
        rectangleFieldName =
            MusicPlaybackControlsTimeBarOnMeasureFingerprint.resultOrThrow().let {
                with(it.mutableMethod) {
                    val rectangleIndex = it.scanResult.patternScanResult!!.startIndex
                    val rectangleReference =
                        getInstruction<ReferenceInstruction>(rectangleIndex).reference
                    (rectangleReference as FieldReference).name
                }
            }

        MusicPlaybackControlsTimeBarDrawFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                // Initialize seekbar method
                addInstructions(
                    1, """
                        move-object/from16 v0, p0
                        const-string v1, "$rectangleFieldName"
                        invoke-static {v0, v1}, $INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;Ljava/lang/String;)V
                        """
                )

                // Draw segment
                val drawCircleIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL
                            && getReference<MethodReference>()?.name == "drawCircle"
                }
                val drawCircleInstruction = getInstruction<FiveRegisterInstruction>(drawCircleIndex)
                addInstruction(
                    drawCircleIndex,
                    "invoke-static {v${drawCircleInstruction.registerC}, v${drawCircleInstruction.registerE}}, " +
                            "$INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
                )
            }
        }

        /**
         * Set current video id
         */
        VideoInformationPatch.videoIdHook("$INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")
    }
}
