package app.revanced.patches.youtube.player.speedoverlay

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.HorizontalTouchOffsetConstructorFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.NextGenWatchLayoutFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.RestoreSlideToSeekBehaviorFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SlideToSeekMotionEventFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayFloatValueFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayTextValueFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.alsoResolve
import app.revanced.util.findMethodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.injectLiteralInstructionBooleanCall
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(dependencies = [SharedResourceIdPatch::class])
object SpeedOverlayPatch : BytecodePatch(
    setOf(
        HorizontalTouchOffsetConstructorFingerprint,
        NextGenWatchLayoutFingerprint,
        RestoreSlideToSeekBehaviorFingerprint,
        SpeedOverlayFingerprint,
        SpeedOverlayFloatValueFingerprint,
        SpeedOverlayTextValueFingerprint,
    )
) {
    override fun execute(context: BytecodeContext) {

        val restoreSlideToSeekBehaviorFingerprintResult =
            RestoreSlideToSeekBehaviorFingerprint.result
        val speedOverlayFingerprintResult = SpeedOverlayFingerprint.result
        val speedOverlayFloatValueFingerprintResult = SpeedOverlayFloatValueFingerprint.result

        val resolvable =
            restoreSlideToSeekBehaviorFingerprintResult != null
                    && speedOverlayFingerprintResult != null
                    && speedOverlayFloatValueFingerprintResult != null

        if (resolvable) {
            // Used on YouTube 18.29.38 ~ YouTube 19.17.41

            // region patch for Disable speed overlay (Enable slide to seek)

            mapOf(
                RestoreSlideToSeekBehaviorFingerprint to 45411329,
                SpeedOverlayFingerprint to 45411330
            ).forEach { (fingerprint, literal) ->
                fingerprint.injectLiteralInstructionBooleanCall(
                    literal,
                    "$PLAYER_CLASS_DESCRIPTOR->disableSpeedOverlay(Z)Z"
                )
            }

            // endregion

            // region patch for Custom speed overlay float value

            speedOverlayFloatValueFingerprintResult!!.let {
                it.mutableMethod.apply {
                    val index = it.scanResult.patternScanResult!!.startIndex
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA

                    addInstructions(
                        index + 1, """
                        invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue(F)F
                        move-result v$register
                        """
                    )
                }
            }

            // endregion

        } else {
            // Used on YouTube 19.18.41~

            // region patch for Disable speed overlay (Enable slide to seek)

            NextGenWatchLayoutFingerprint.resultOrThrow().mutableMethod.apply {
                val booleanValueIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "booleanValue"
                }
                val insertIndex = indexOfFirstInstructionOrThrow(booleanValueIndex - 10) {
                    opcode == Opcode.IGET_OBJECT
                            && getReference<FieldReference>()?.definingClass == definingClass
                }
                val insertInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
                val insertReference = getInstruction<ReferenceInstruction>(insertIndex).reference

                addInstruction(
                    insertIndex + 1,
                    "iget-object v${insertInstruction.registerA}, v${insertInstruction.registerB}, $insertReference"
                )

                val jumpIndex = indexOfFirstInstructionOrThrow(booleanValueIndex) {
                    opcode == Opcode.IGET_OBJECT
                            && getReference<FieldReference>()?.definingClass == definingClass
                }

                hook(insertIndex + 1, insertInstruction.registerA, jumpIndex)
            }

            val (slideToSeekBooleanMethod, slideToSeekSyntheticMethod) =
                SlideToSeekMotionEventFingerprint.alsoResolve(
                    context, HorizontalTouchOffsetConstructorFingerprint
                ).let {
                    with(it.mutableMethod) {
                        val scanResult = it.scanResult.patternScanResult!!
                        val jumpIndex = scanResult.endIndex + 1
                        val insertIndex = scanResult.endIndex - 1
                        val insertRegister =
                            getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                        hook(insertIndex, insertRegister, jumpIndex)

                        val slideToSeekBooleanMethod =
                            getWalkerMethod(context, scanResult.startIndex + 1)

                        val slideToSeekConstructorMethod =
                            context.findMethodOrThrow(slideToSeekBooleanMethod.definingClass)

                        val slideToSeekSyntheticIndex = slideToSeekConstructorMethod
                            .indexOfFirstInstructionReversedOrThrow {
                                opcode == Opcode.NEW_INSTANCE
                            }

                        val slideToSeekSyntheticClass = slideToSeekConstructorMethod
                            .getInstruction<ReferenceInstruction>(slideToSeekSyntheticIndex)
                            .reference
                            .toString()

                        val slideToSeekSyntheticMethod =
                            context.findMethodOrThrow(slideToSeekSyntheticClass) {
                                name == "run"
                            }

                        Pair(slideToSeekBooleanMethod, slideToSeekSyntheticMethod)
                    }
                }

            slideToSeekBooleanMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET_OBJECT
                }
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL
                }

                hook(insertIndex, insertRegister, jumpIndex)
            }

            slideToSeekSyntheticMethod.apply {
                val speedOverlayFloatValueIndex = indexOfFirstInstructionOrThrow {
                    (this as? NarrowLiteralInstruction)?.narrowLiteral == 2.0f.toRawBits()
                }
                val insertIndex =
                    indexOfFirstInstructionReversedOrThrow(speedOverlayFloatValueIndex) {
                        getReference<MethodReference>()?.name == "removeCallbacks"
                    } + 1
                val insertRegister =
                    getInstruction<FiveRegisterInstruction>(insertIndex - 1).registerC
                val jumpIndex =
                    indexOfFirstInstructionOrThrow(
                        speedOverlayFloatValueIndex,
                        Opcode.RETURN_VOID
                    ) + 1

                hook(insertIndex, insertRegister, jumpIndex)
            }

            // endregion

            // region patch for Custom speed overlay float value

            slideToSeekSyntheticMethod.apply {
                val speedOverlayFloatValueIndex = indexOfFirstInstructionOrThrow {
                    (this as? NarrowLiteralInstruction)?.narrowLiteral == 2.0f.toRawBits()
                }
                val speedOverlayFloatValueRegister =
                    getInstruction<OneRegisterInstruction>(speedOverlayFloatValueIndex).registerA

                addInstructions(
                    speedOverlayFloatValueIndex + 1, """
                        invoke-static {v$speedOverlayFloatValueRegister}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue(F)F
                        move-result v$speedOverlayFloatValueRegister
                        """
                )
            }

            SpeedOverlayTextValueFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue()D
                            move-result-wide v$targetRegister
                            """
                    )
                }
            }

            // endregion

        }
    }

    // restore slide to seek
    private fun MutableMethod.hook(
        insertIndex: Int,
        insertRegister: Int,
        jumpIndex: Int
    ) {
        addInstructionsWithLabels(
            insertIndex,
            """
                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableSpeedOverlay()Z
                move-result v$insertRegister
                if-eqz v$insertRegister, :disable
                """, ExternalLabel("disable", getInstruction(jumpIndex))
        )
    }
}
