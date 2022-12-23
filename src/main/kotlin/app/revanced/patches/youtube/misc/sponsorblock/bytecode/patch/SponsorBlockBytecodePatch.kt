package app.revanced.patches.youtube.misc.sponsorblock.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.playercontrols.bytecode.patch.PlayerControlsBytecodePatch
import app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.MainstreamVideoIdPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.patches.timebar.HookTimebarPatch
import app.revanced.shared.util.bytecode.BytecodeHelper
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction22c
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.Opcode

@Name("sponsorblock-bytecode-patch")
@DependsOn(
    [
        MainstreamVideoIdPatch::class,
        PlayerControlsBytecodePatch::class,
        SharedResourcdIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SponsorBlockBytecodePatch : BytecodePatch(
    listOf(
        NextGenWatchLayoutFingerprint,
        AppendTimeFingerprint,
        PlayerOverlaysLayoutInitFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /*
         inject MainstreamVideoIdPatch
         */
        MainstreamVideoIdPatch.injectCall("$INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setCurrentVideoId(Ljava/lang/String;)V")

        /*
         Seekbar drawing
         */
        insertResult = HookTimebarPatch.SetTimbarFingerprintResult
        insertMethod = insertResult.mutableMethod
        val insertInstructions = insertMethod.implementation!!.instructions

        /*
         Get the instance of the seekbar rectangle
         */
        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.IGET_OBJECT) continue
            val seekbarRegister = (instruction as Instruction22c).registerB
            insertMethod.addInstruction(
                index - 1,
                "invoke-static {v$seekbarRegister}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;)V"
            )
            break
        }

        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.INVOKE_STATIC) continue

            val invokeInstruction = instruction as Instruction35c
            if ((invokeInstruction.reference as MethodReference).name != "round") continue

            val insertIndex = index + 2

            // set the thickness of the segment
            insertMethod.addInstruction(
                insertIndex,
                "invoke-static {v${invokeInstruction.registerC}}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )
            break
        }

        /*
        Set rectangle absolute left and right positions
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
        Draw segment
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
        Voting & Shield button
         */

        arrayOf("ShieldButton", "VotingButton").forEach { button ->
           PlayerControlsBytecodePatch.initializeSB("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/$button;")
           PlayerControlsBytecodePatch.injectVisibility("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/$button;")
           PlayerControlsBytecodePatch.injectVisibilityNegated("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/$button;")
        }

        // set SegmentHelperLayout.context to the player layout instance
        val instanceRegister = 0
        NextGenWatchLayoutFingerprint.result!!.mutableMethod.addInstruction(
            3, // after super call
            "invoke-static/range {p$instanceRegister}, Lapp/revanced/integrations/sponsorblock/PlayerController;->addSkipSponsorView15(Landroid/view/View;)V"
        )

        BytecodeHelper.injectInit(context, "FirstRun", "initializationSB")
        BytecodeHelper.patchStatus(context, "Sponsorblock")

        return PatchResultSuccess()
    }

    internal companion object {
        const val INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR =
            "Lapp/revanced/integrations/sponsorblock"

        const val INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR =
            "$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/PlayerController;"

        lateinit var insertResult: MethodFingerprintResult
        lateinit var insertMethod: MutableMethod

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