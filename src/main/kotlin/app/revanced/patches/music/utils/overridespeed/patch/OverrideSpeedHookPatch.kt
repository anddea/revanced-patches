package app.revanced.patches.music.utils.overridespeed.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.utils.overridespeed.fingerprints.PlaybackSpeedFingerprint
import app.revanced.patches.music.utils.overridespeed.fingerprints.PlaybackSpeedParentFingerprint
import app.revanced.patches.music.utils.overridespeed.fingerprints.PlaybackSpeedPatchFingerprint
import app.revanced.util.integrations.Constants.MUSIC_INTEGRATIONS_PATH
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedMethodReference
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.immutable.ImmutableField

class OverrideSpeedHookPatch : BytecodePatch(
    listOf(
        PlaybackSpeedPatchFingerprint,
        PlaybackSpeedParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        PlaybackSpeedParentFingerprint.result?.let { parentResult ->
            val parentClassDef = parentResult.classDef

            PlaybackSpeedFingerprint.also {
                it.resolve(
                    context,
                    parentClassDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val endIndex = it.scanResult.patternScanResult!!.endIndex

                    val speedClassRegister =
                        getInstruction<OneRegisterInstruction>(startIndex).registerA
                    val speedRegister =
                        getInstruction<OneRegisterInstruction>(startIndex + 1).registerA

                    SPEED_REFERENCE = getInstruction<BuilderInstruction35c>(endIndex).reference
                    SPEED_CLASS = (SPEED_REFERENCE as DexBackedMethodReference).definingClass

                    with(
                        context
                            .toMethodWalker(this)
                            .nextMethod(endIndex, true)
                            .getMethod() as MutableMethod
                    ) {
                        addInstruction(
                            this.implementation!!.instructions.size - 1,
                            "sput p1, $INTEGRATIONS_VIDEO_HELPER_CLASS_DESCRIPTOR->currentSpeed:F"
                        )
                    }

                    addInstructions(
                        startIndex + 2, """
                            sput-object v$speedClassRegister, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->speedClass:$SPEED_CLASS
                            invoke-static {}, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeed()F
                            move-result v$speedRegister
                            """
                    )
                }

            } ?: throw PlaybackSpeedFingerprint.exception
        } ?: throw PlaybackSpeedParentFingerprint.exception

        PlaybackSpeedPatchFingerprint.result?.let {
            it.mutableMethod.apply {
                it.mutableClass.staticFields.add(
                    ImmutableField(
                        definingClass,
                        "speedClass",
                        SPEED_CLASS,
                        AccessFlags.PUBLIC or AccessFlags.STATIC,
                        null,
                        annotations,
                        null
                    ).toMutable()
                )

                addInstructions(
                    4, """
                        sget-object v0, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->speedClass:$SPEED_CLASS
                        invoke-virtual {v0, p0}, $SPEED_REFERENCE
                        """
                )
            }

        } ?: throw PlaybackSpeedPatchFingerprint.exception

    }

    internal companion object {
        const val INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
            "$MUSIC_MISC_PATH/PlaybackSpeedPatch;"

        const val INTEGRATIONS_VIDEO_HELPER_CLASS_DESCRIPTOR =
            "$MUSIC_INTEGRATIONS_PATH/utils/VideoHelpers;"

        private lateinit var SPEED_CLASS: String
        private lateinit var SPEED_REFERENCE: Reference
    }
}