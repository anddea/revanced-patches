package app.revanced.patches.music.utils.overridequality.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patches.music.utils.overridequality.fingerprints.VideoQualityListFingerprint
import app.revanced.patches.music.utils.overridequality.fingerprints.VideoQualityPatchFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.MUSIC_VIDEO_PATH
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.util.MethodUtil

@DependsOn([SharedResourceIdPatch::class])
class OverrideQualityHookPatch : BytecodePatch(
    listOf(
        VideoQualityListFingerprint,
        VideoQualityPatchFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        VideoQualityListFingerprint.result?.let {
            val constructorMethod = it.mutableClass.methods.first { method -> MethodUtil.isConstructor(method) }
            val overrideMethod = it.mutableClass.methods.find { method -> method.parameterTypes.first() == "I" }

            QUALITY_CLASS = it.method.definingClass
            QUALITY_METHOD = overrideMethod?.name
                ?:throw PatchException("Failed to find hook method")

            constructorMethod.apply {
                addInstruction(
                    2,
                    "sput-object p0, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->qualityClass:$QUALITY_CLASS"
                )
            }

            it.mutableMethod.apply {
                val listIndex = it.scanResult.patternScanResult!!.startIndex
                val listRegister = getInstruction<FiveRegisterInstruction>(listIndex).registerD

                addInstruction(
                    listIndex,
                    "invoke-static {v$listRegister}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQualityList([Ljava/lang/Object;)V"
                )
            }
        } ?: throw VideoQualityListFingerprint.exception

        VideoQualityPatchFingerprint.result?.let {
            it.mutableMethod.apply {
                it.mutableClass.staticFields.add(
                    ImmutableField(
                        definingClass,
                        "qualityClass",
                        QUALITY_CLASS,
                        AccessFlags.PUBLIC or AccessFlags.STATIC,
                        null,
                        annotations,
                        null
                    ).toMutable()
                )

                addInstructions(
                    0, """
                        sget-object v0, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->qualityClass:$QUALITY_CLASS
                        invoke-virtual {v0, p0}, $QUALITY_CLASS->$QUALITY_METHOD(I)V
                        """
                )
            }
        } ?: throw VideoQualityPatchFingerprint.exception
    }

    internal companion object {
        const val INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR =
            "$MUSIC_VIDEO_PATH/VideoQualityPatch;"

        private lateinit var QUALITY_CLASS: String
        private lateinit var QUALITY_METHOD: String
    }
}