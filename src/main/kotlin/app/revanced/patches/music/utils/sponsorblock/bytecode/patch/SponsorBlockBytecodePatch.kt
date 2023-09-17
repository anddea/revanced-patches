package app.revanced.patches.music.utils.sponsorblock.bytecode.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.utils.fingerprints.SeekBarConstructorFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.sponsorblock.bytecode.fingerprints.MusicPlaybackControlsTimeBarDrawFingerprint
import app.revanced.patches.music.utils.sponsorblock.bytecode.fingerprints.MusicPlaybackControlsTimeBarOnMeasureFingerprint
import app.revanced.patches.music.utils.sponsorblock.bytecode.fingerprints.SeekbarOnDrawFingerprint
import app.revanced.patches.music.video.information.patch.VideoInformationPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@DependsOn(
    [
        SharedResourceIdPatch::class,
        VideoInformationPatch::class
    ]
)
class SponsorBlockBytecodePatch : BytecodePatch(
    listOf(
        MusicPlaybackControlsTimeBarDrawFingerprint,
        MusicPlaybackControlsTimeBarOnMeasureFingerprint,
        SeekBarConstructorFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Hook the video time methods & Initialize the player controller
         */
        VideoInformationPatch.apply {
            videoTimeHook(
                INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
                "setVideoTime"
            )
            onCreateHook(
                INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
                "initialize"
            )
        }


        /**
         * Responsible for seekbar in fullscreen
         */
        SeekBarConstructorFingerprint.result?.classDef?.let { classDef ->
            SeekbarOnDrawFingerprint.also {
                it.resolve(
                    context,
                    classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    // Initialize seekbar method
                    addInstructions(
                        0, """
                            move-object/from16 v0, p0
                            const-string v1, "${VideoInformationPatch.rectangleFieldName}"
                            invoke-static {v0, v1}, $INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;Ljava/lang/String;)V
                            """
                    )

                    // Set seekbar thickness
                    for ((index, instruction) in implementation!!.instructions.withIndex()) {
                        if (instruction.opcode != Opcode.INVOKE_STATIC) continue

                        val invokeInstruction = getInstruction<Instruction35c>(index)
                        if ((invokeInstruction.reference as MethodReference).name != "round") continue

                        val insertIndex = index + 2

                        addInstruction(
                            insertIndex,
                            "invoke-static {v${invokeInstruction.registerC}}, $INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
                        )
                        break
                    }

                    // Draw segment
                    for ((index, instruction) in implementation!!.instructions.withIndex()) {
                        if (instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE) continue

                        val invokeInstruction = instruction as BuilderInstruction3rc
                        if ((invokeInstruction.reference as MethodReference).name != "restore") continue

                        val drawSegmentInstructionInsertIndex = index - 1

                        val (canvasInstance, centerY) =
                            getInstruction<FiveRegisterInstruction>(
                                drawSegmentInstructionInsertIndex
                            ).let { drawSegmentInstruction ->
                                drawSegmentInstruction.registerC to drawSegmentInstruction.registerE
                            }

                        addInstruction(
                            drawSegmentInstructionInsertIndex,
                            "invoke-static {v$canvasInstance, v$centerY}, $INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
                        )
                        break
                    }
                }
            } ?: throw SeekbarOnDrawFingerprint.exception
        } ?: throw SeekBarConstructorFingerprint.exception


        /**
         * Responsible for seekbar in player
         */
        MusicPlaybackControlsTimeBarOnMeasureFingerprint.result?.let {
            it.mutableMethod.apply {
                val rectangleIndex = it.scanResult.patternScanResult!!.startIndex
                val rectangleReference =
                    getInstruction<ReferenceInstruction>(rectangleIndex).reference
                rectangleFieldName = (rectangleReference as FieldReference).name
            }
        } ?: throw MusicPlaybackControlsTimeBarOnMeasureFingerprint.exception

        MusicPlaybackControlsTimeBarDrawFingerprint.result?.let {
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
                for ((index, instruction) in implementation!!.instructions.withIndex()) {
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) continue

                    val invokeInstruction = getInstruction<Instruction35c>(index)
                    if ((invokeInstruction.reference as MethodReference).name != "drawCircle") continue

                    val (canvasInstance, centerY) =
                        getInstruction<FiveRegisterInstruction>(
                            index
                        ).let { drawSegmentInstruction ->
                            drawSegmentInstruction.registerC to drawSegmentInstruction.registerE
                        }

                    addInstruction(
                        index,
                        "invoke-static {v$canvasInstance, v$centerY}, $INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
                    )
                    break
                }
            }
        } ?: throw MusicPlaybackControlsTimeBarDrawFingerprint.exception

        /**
         * Set current video id
         */
        VideoInformationPatch.injectCall("$INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setCurrentVideoId(Ljava/lang/String;)V")
    }

    private companion object {
        const val INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR =
            "Lapp/revanced/music/sponsorblock/SegmentPlaybackController;"

        lateinit var rectangleFieldName: String
    }
}
