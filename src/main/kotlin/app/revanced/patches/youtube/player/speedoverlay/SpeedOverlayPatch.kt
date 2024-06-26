package app.revanced.patches.youtube.player.speedoverlay

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.HorizontalTouchOffsetConstructorFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.NextGenWatchLayoutFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.RestoreSlideToSeekBehaviorFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SlideToSeekMotionEventFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayTextValueFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayValueFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.getReference
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getTargetIndexReversedOrThrow
import app.revanced.util.getTargetIndexWithMethodReferenceNameOrThrow
import app.revanced.util.getTargetIndexWithMethodReferenceNameReversedOrThrow
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Patch(dependencies = [SharedResourceIdPatch::class])
object SpeedOverlayPatch : BytecodePatch(
    setOf(
        HorizontalTouchOffsetConstructorFingerprint,
        NextGenWatchLayoutFingerprint,
        RestoreSlideToSeekBehaviorFingerprint,
        SpeedOverlayFingerprint,
        SpeedOverlayTextValueFingerprint,
        SpeedOverlayValueFingerprint,
    )
) {
    override fun execute(context: BytecodeContext) {

        val restoreSlideToSeekBehaviorFingerprintResult =
            RestoreSlideToSeekBehaviorFingerprint.result
        val speedOverlayFingerprintResult = SpeedOverlayFingerprint.result
        val speedOverlayValueFingerprintResult = SpeedOverlayValueFingerprint.result

        val resolvable =
            restoreSlideToSeekBehaviorFingerprintResult != null
                    && speedOverlayFingerprintResult != null
                    && speedOverlayValueFingerprintResult != null

        if (resolvable) {
            // Legacy method.
            // Used on YouTube 18.29.38 ~ YouTube 19.17.41

            // region patch for disable speed overlay

            mapOf(
                RestoreSlideToSeekBehaviorFingerprint to 45411329,
                SpeedOverlayFingerprint to 45411330
            ).forEach { (fingerprint, literal) ->
                fingerprint.result!!.let {
                    fingerprint.literalInstructionBooleanHook(
                        literal,
                        "$PLAYER_CLASS_DESCRIPTOR->disableSpeedOverlay(Z)Z"
                    )
                }
            }

            // endregion

            // region patch for custom speed overlay value

            speedOverlayValueFingerprintResult!!.let {
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
            // New method.
            // Used on YouTube 19.18.41~

            NextGenWatchLayoutFingerprint.resultOrThrow().mutableMethod.apply {
                val booleanValueIndex = getTargetIndexWithMethodReferenceNameOrThrow("booleanValue")

                val insertIndex = findIGetIndex(booleanValueIndex - 10, booleanValueIndex)
                val insertInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
                val insertReference = getInstruction<ReferenceInstruction>(insertIndex).reference

                addInstruction(
                    insertIndex + 1,
                    "iget-object v${insertInstruction.registerA}, v${insertInstruction.registerB}, $insertReference"
                )

                val jumpIndex = findIGetIndex(booleanValueIndex, booleanValueIndex + 10)

                hook(insertIndex + 1, insertInstruction.registerA, jumpIndex)
            }

            SlideToSeekMotionEventFingerprint.resolve(
                context,
                HorizontalTouchOffsetConstructorFingerprint.resultOrThrow().classDef
            )
            SlideToSeekMotionEventFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val scanResult = it.scanResult.patternScanResult!!

                    val slideToSeekBooleanIndex = scanResult.startIndex + 1
                    slideToSeekBooleanMethod = getWalkerMethod(context, slideToSeekBooleanIndex)

                    val jumpIndex = scanResult.endIndex + 1
                    val insertIndex = scanResult.endIndex - 1
                    val insertRegister =
                        getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                    hook(insertIndex, insertRegister, jumpIndex)
                }
            }

            slideToSeekBooleanMethod.apply {
                var insertIndex = getTargetIndexOrThrow(Opcode.IGET_OBJECT)
                var insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                var jumpIndex = getTargetIndexReversedOrThrow(Opcode.INVOKE_VIRTUAL)

                hook(insertIndex, insertRegister, jumpIndex)

                val constructorMethod =
                    context.findClass(definingClass)?.mutableClass
                        ?.methods?.find { method -> method.name == "<init>" }
                        ?: throw PatchException("Could not find constructor method")

                constructorMethod.apply {
                    val syntheticIndex = getTargetIndexReversedOrThrow(Opcode.NEW_INSTANCE)
                    val syntheticClass =
                        getInstruction<ReferenceInstruction>(syntheticIndex).reference.toString()

                    val syntheticMethod =
                        context.findClass(syntheticClass)?.mutableClass
                            ?.methods?.find { method -> method.name == "run" }
                            ?: throw PatchException("Could not find synthetic method")

                    syntheticMethod.apply {
                        val speedOverlayValueIndex =
                            indexOfFirstInstructionOrThrow { (this as? NarrowLiteralInstruction)?.narrowLiteral == 2.0f.toRawBits() }
                        val speedOverlayValueRegister =
                            getInstruction<OneRegisterInstruction>(speedOverlayValueIndex).registerA

                        addInstructions(
                            speedOverlayValueIndex + 1, """
                                invoke-static {v$speedOverlayValueRegister}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue(F)F
                                move-result v$speedOverlayValueRegister
                                """
                        )

                        insertIndex = getTargetIndexWithMethodReferenceNameReversedOrThrow(
                            speedOverlayValueIndex,
                            "removeCallbacks"
                        ) + 1
                        insertRegister =
                            getInstruction<FiveRegisterInstruction>(insertIndex - 1).registerC
                        jumpIndex =
                            getTargetIndexOrThrow(speedOverlayValueIndex, Opcode.RETURN_VOID) + 1
                        hook(insertIndex, insertRegister, jumpIndex)
                    }
                }
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
        }
    }

    private lateinit var slideToSeekBooleanMethod: MutableMethod

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

    private fun MutableMethod.findIGetIndex(
        startIndex: Int,
        endIndex: Int
    ): Int = implementation!!.instructions.let { instruction ->
        startIndex + instruction.subList(startIndex, endIndex).indexOfFirst {
            it.opcode == Opcode.IGET_OBJECT
                    && it.getReference<FieldReference>()?.definingClass == definingClass
        }
    }
}
