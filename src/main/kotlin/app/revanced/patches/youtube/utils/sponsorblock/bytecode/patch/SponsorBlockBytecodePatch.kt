package app.revanced.patches.youtube.utils.sponsorblock.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.SeekbarFingerprint
import app.revanced.patches.youtube.utils.fingerprints.SeekbarOnDrawFingerprint
import app.revanced.patches.youtube.utils.fingerprints.TotalTimeFingerprint
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.overridespeed.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.playercontrols.patch.PlayerControlsPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.InsetOverlayViewLayout
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.TotalTime
import app.revanced.patches.youtube.utils.sponsorblock.bytecode.fingerprints.EndScreenEngagementPanelsFingerprint
import app.revanced.patches.youtube.utils.sponsorblock.bytecode.fingerprints.PlayerControllerFingerprint
import app.revanced.patches.youtube.utils.sponsorblock.bytecode.fingerprints.RectangleFieldInvalidatorFingerprint
import app.revanced.patches.youtube.utils.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.patches.youtube.utils.videoid.mainstream.patch.MainstreamVideoIdPatch
import app.revanced.util.bytecode.BytecodeHelper.injectInit
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import app.revanced.util.bytecode.getWideLiteralIndex
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.instruction.BuilderInstruction3rc
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@Name("sponsorblock-bytecode-patch")
@DependsOn(
    [
        LegacyVideoIdPatch::class,
        MainstreamVideoIdPatch::class,
        OverrideSpeedHookPatch::class,
        PlayerControlsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SponsorBlockBytecodePatch : BytecodePatch(
    listOf(
        EndScreenEngagementPanelsFingerprint,
        PlayerControllerFingerprint,
        TotalTimeFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /**
         * Hook the video time methods
         */
        MainstreamVideoIdPatch.apply {
            videoTimeHook(
                INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR,
                "setVideoTime"
            )
            onCreateHook(
                INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR,
                "initialize"
            )
        }


        /**
         * Seekbar drawing
         */
        insertMethod = SeekbarFingerprint.result!!.let {
            SeekbarOnDrawFingerprint.apply { resolve(context, it.mutableClass) }
        }.result?.mutableMethod ?: return SeekbarFingerprint.toErrorResult()
        insertInstructions = insertMethod.implementation!!.instructions


        /**
         * Get left and right of seekbar rectangle
         */
        val moveRectangleToRegisterIndex = insertInstructions.indexOfFirst {
            it.opcode == Opcode.MOVE_OBJECT_FROM16
        }

        insertMethod.addInstruction(
            moveRectangleToRegisterIndex + 1,
            "invoke-static/range {p0 .. p0}, " +
                    "$INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;)V"
        )

        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.INVOKE_STATIC) continue

            val invokeInstruction = insertMethod.getInstruction<Instruction35c>(index)
            if ((invokeInstruction.reference as MethodReference).name != "round") continue

            val insertIndex = index + 2

            insertMethod.addInstruction(
                insertIndex,
                "invoke-static {v${invokeInstruction.registerC}}, " +
                        "$INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )
            break
        }

        /**
         * Draw segment
         */
        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE) continue

            val invokeInstruction = instruction as BuilderInstruction3rc
            if ((invokeInstruction.reference as MethodReference).name != "restore") continue

            val drawSegmentInstructionInsertIndex = index - 1

            val (canvasInstance, centerY) =
                insertMethod.getInstruction<FiveRegisterInstruction>(
                    drawSegmentInstructionInsertIndex
                ).let { it.registerC to it.registerE }

            insertMethod.addInstruction(
                drawSegmentInstructionInsertIndex,
                "invoke-static {v$canvasInstance, v$centerY}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
            )
            break
        }

        /**
         * Voting & Shield button
         */
        arrayOf("CreateSegmentButtonController", "VotingButtonController").forEach {
            PlayerControlsPatch.initializeSB("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/ui/$it;")
            PlayerControlsPatch.injectVisibility("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/ui/$it;")
        }

        EndScreenEngagementPanelsFingerprint.result?.mutableMethod?.let {
            it.addInstruction(
                it.implementation!!.instructions.size - 1,
                "invoke-static {}, $INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/ui/SponsorBlockViewController;->endOfVideoReached()V"
            )
        }

        /**
         * Append the new time to the player layout
         */
        TotalTimeFingerprint.result?.mutableMethod?.let {
            it.apply {
                val targetIndex = getWideLiteralIndex(TotalTime) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->appendTimeWithoutSegments(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        } ?: return TotalTimeFingerprint.toErrorResult()

        /**
         * Initialize the SponsorBlock view
         */
        YouTubeControlsOverlayFingerprint.result?.mutableMethod?.let {
            it.apply {
                val targetIndex = getWideLiteralIndex(InsetOverlayViewLayout) + 3
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/ui/SponsorBlockViewController;->initialize(Landroid/view/ViewGroup;)V"
                )
            }
        } ?: return YouTubeControlsOverlayFingerprint.toErrorResult()

        /**
         * Replace strings
         */
        RectangleFieldInvalidatorFingerprint.resolve(
            context,
            SeekbarOnDrawFingerprint.result!!.classDef
        )
        val rectangleFieldInvalidatorInstructions =
            RectangleFieldInvalidatorFingerprint.result!!.method.implementation!!.instructions
        val rectangleFieldName =
            ((rectangleFieldInvalidatorInstructions.elementAt(rectangleFieldInvalidatorInstructions.count() - 3) as ReferenceInstruction).reference as FieldReference).name


        PlayerControllerFingerprint.result?.mutableMethod?.let {
            val instructions = it.implementation!!.instructions

            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode != Opcode.CONST_STRING) continue
                val register = it.getInstruction<OneRegisterInstruction>(index).registerA
                it.replaceInstruction(
                    index,
                    "const-string v$register, \"$rectangleFieldName\""
                )
                break
            }
        } ?: return PlayerControllerFingerprint.toErrorResult()

        /**
         * Inject VideoIdPatch
         */
        LegacyVideoIdPatch.injectCall("$INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setCurrentVideoId(Ljava/lang/String;)V")

        context.injectInit("FirstRun", "initializationSB")
        context.updatePatchStatus("SponsorBlock")

        return PatchResultSuccess()
    }

    internal companion object {
        const val INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR =
            "Lapp/revanced/integrations/sponsorblock"

        const val INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR =
            "$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/SegmentPlaybackController;"

        lateinit var insertMethod: MutableMethod
        lateinit var insertInstructions: List<BuilderInstruction>
    }
}