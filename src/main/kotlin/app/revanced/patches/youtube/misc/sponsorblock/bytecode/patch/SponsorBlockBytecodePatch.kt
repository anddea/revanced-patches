package app.revanced.patches.youtube.misc.sponsorblock.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.overridespeed.bytecode.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.misc.playercontrols.patch.PlayerControlsPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.timebar.patch.HookTimebarPatch
import app.revanced.patches.youtube.misc.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.MainstreamVideoIdPatch
import app.revanced.util.bytecode.BytecodeHelper.injectInit
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
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
        NextGenWatchLayoutFingerprint,
        PlayerControllerFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /*
         * Hook the video time methods
         */
        with(MainstreamVideoIdPatch) {
            videoTimeHook(
                INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR,
                "setVideoTime"
            )
            highPrecisionTimeHook(
                INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR,
                "setHighPrecisionVideoTime"
            )
        }

        /*
         * Inject VideoIdPatch
         */
        LegacyVideoIdPatch.injectCall("$INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setCurrentVideoId(Ljava/lang/String;)V")
        MainstreamVideoIdPatch.injectCall("$INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setCurrentVideoId(Ljava/lang/String;)V")


        /*
         * Seekbar drawing
         */
        insertMethod = HookTimebarPatch.setTimebarMethod
        insertInstructions = insertMethod.implementation!!.instructions


        /*
         * Get the instance of the seekbar rectangle
         */
        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.INVOKE_DIRECT_RANGE) continue
            insertMethod.addInstruction(
                index,
                "invoke-static/range {p0 .. p0}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;)V"
            )
            break
        }

        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.INVOKE_STATIC) continue

            val invokeInstruction = instruction as Instruction35c
            if ((invokeInstruction.reference as MethodReference).name != "round") continue

            val insertIndex = index + 2

            insertMethod.addInstruction(
                insertIndex,
                "invoke-static {v${invokeInstruction.registerC}}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )
            break
        }

        /*
         * Set rectangle absolute left and right positions
         */
        val drawRectangleInstructions = insertInstructions.filter {
            it is ReferenceInstruction && (it.reference as? MethodReference)?.name == "drawRect" && it is FiveRegisterInstruction
        }.map { // TODO: improve code
            insertInstructions.indexOf(it) to (it as FiveRegisterInstruction).registerD
        }

        mapOf(
            "setSponsorBarAbsoluteLeft" to 3,
            "setSponsorBarAbsoluteRight" to 0
        ).forEach { (string, int) ->
            val (index, register) = drawRectangleInstructions[int]
            injectCallRectangle(index, register, string)
        }

        /*
         * Draw segment
         */
        val drawSegmentInstructionInsertIndex = (insertInstructions.size - 1 - 2)
        val (canvasInstance, centerY) = (insertInstructions[drawSegmentInstructionInsertIndex] as FiveRegisterInstruction).let {
            it.registerC to it.registerE
        }
        insertMethod.addInstruction(
            drawSegmentInstructionInsertIndex,
            "invoke-static {v$canvasInstance, v$centerY}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
        )

        /*
         * Voting & Shield button
         */

        arrayOf("ShieldButton", "VotingButton").forEach {
           PlayerControlsPatch.initializeSB("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/$it;")
           PlayerControlsPatch.injectVisibility("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/$it;")
        }

        // set SegmentHelperLayout.context to the player layout instance
        val instanceRegister = 0
        NextGenWatchLayoutFingerprint.result?.mutableMethod?.addInstruction(
            3,
            "invoke-static/range {p$instanceRegister}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->addSkipSponsorView15(Landroid/view/View;)V"
        ) ?: return NextGenWatchLayoutFingerprint.toErrorResult()


        /*
         * Replace strings
         */
        PlayerControllerFingerprint.result?.mutableMethod?.let {
            val instructions = it.implementation!!.instructions

            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode != Opcode.CONST_STRING) continue
                val register = (instruction as OneRegisterInstruction).registerA
                it.replaceInstruction(
                    index,
                    "const-string v$register, \"${MainstreamVideoIdPatch.reactReference}\""
                )
                break
            }
        } ?: return PlayerControllerFingerprint.toErrorResult()

        context.injectInit("FirstRun", "initializationSB")
        context.updatePatchStatus("SponsorBlock")

        return PatchResultSuccess()
    }

    internal companion object {
        const val INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR =
            "Lapp/revanced/integrations/sponsorblock"

        const val INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR =
            "$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/PlayerController;"

        lateinit var insertMethod: MutableMethod
        lateinit var insertInstructions: List<BuilderInstruction>

        /**
         * Adds an invoke-static instruction, called with the new id when the video changes
         * @param methodDescriptor which method to call. Params have to be `Ljava/lang/String;`
         */
        fun injectCallRectangle(
            insertIndex: Int,
            targetRegister: Int,
            methodDescriptor: String
        ) {
            insertMethod.addInstruction(
                insertIndex,
                "invoke-static {v$targetRegister}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->$methodDescriptor(Landroid/graphics/Rect;)V"
            )
        }
    }
}