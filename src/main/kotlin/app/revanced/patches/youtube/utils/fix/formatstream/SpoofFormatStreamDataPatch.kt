package app.revanced.patches.youtube.utils.fix.formatstream

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.compatibility.Constants
import app.revanced.patches.youtube.utils.fix.formatstream.fingerprints.FormatStreamModelConstructorFingerprint
import app.revanced.patches.youtube.utils.fix.formatstream.fingerprints.PlaybackStartFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.patches.youtube.video.playerresponse.PlayerResponseMethodHookPatch
import app.revanced.patches.youtube.video.videoid.VideoIdPatch
import app.revanced.util.getReference
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object SpoofFormatStreamDataPatch : BaseBytecodePatch(
    name = "Spoof format stream data",
    description = "Adds options to spoof format stream data to prevent playback issues.",
    dependencies = setOf(
        PlayerResponseMethodHookPatch::class,
        SettingsPatch::class,
        VideoIdPatch::class,
        VideoInformationPatch::class,
    ),
    compatiblePackages = Constants.COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        FormatStreamModelConstructorFingerprint,
        PlaybackStartFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/SpoofFormatStreamDataPatch;"

    private const val INTEGRATIONS_METHOD_DESCRIPTOR =
        "hookStreamData"

    private const val INTEGRATIONS_METHOD_CALL =
        INTEGRATIONS_CLASS_DESCRIPTOR +
                "->" +
                INTEGRATIONS_METHOD_DESCRIPTOR +
                "(Ljava/lang/Object;)V"

    private const val STREAMING_DATA_OUTER_CLASS =
        "Lcom/google/protos/youtube/api/innertube/StreamingDataOuterClass\$StreamingData;"

    private lateinit var hookMethod: MutableMethod

    private fun MutableMethod.replaceFieldName(
        index: Int,
        replaceFieldString: String
    ) {
        val reference = getInstruction<ReferenceInstruction>(index).reference
        val fieldName = (reference as FieldReference).name

        hookMethod.apply {
            val stringIndex = getStringInstructionIndex(replaceFieldString)
            val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

            replaceInstruction(
                stringIndex,
                "const-string v$stringRegister, \"$fieldName\""
            )
        }
    }

    override fun execute(context: BytecodeContext) {

        // Hook player response video id, to start loading format stream data sooner in the background.
        VideoIdPatch.hookPlayerResponseVideoId("$INTEGRATIONS_CLASS_DESCRIPTOR->newPlayerResponseVideoId(Ljava/lang/String;Z)V")

        hookMethod = context.findClass(INTEGRATIONS_CLASS_DESCRIPTOR)!!
            .mutableClass.methods.find { method -> method.name == INTEGRATIONS_METHOD_DESCRIPTOR }
            ?: throw PatchException("SpoofFormatStreamDataPatch not found")

        FormatStreamModelConstructorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {

                // Find the field name that will be used for reflection.
                val urlIndex = it.scanResult.patternScanResult!!.startIndex
                val itagIndex = getTargetIndex(urlIndex + 1, Opcode.IGET)
                val audioCodecParameterIndex = getTargetIndex(urlIndex + 1, Opcode.IGET_OBJECT)

                replaceFieldName(urlIndex, "replaceMeWithUrlFieldName")
                replaceFieldName(itagIndex, "replaceMeWithITagFieldName")
                replaceFieldName(audioCodecParameterIndex, "replaceMeWithAudioCodecParameterFieldName")
            }
        }

        PlaybackStartFingerprint.resultOrThrow().mutableMethod.apply {

            // Type of object being invoked is protobufList.
            // Find the class name of protobufList.
            val protobufListIndex = indexOfFirstInstruction {
                opcode == Opcode.INVOKE_STATIC
                        && getReference<MethodReference>()?.definingClass == STREAMING_DATA_OUTER_CLASS
                        && getReference<MethodReference>()?.name == "emptyProtobufList"
            }
            if (protobufListIndex <= 0)
                throw PatchException("ProtobufList index not found")

            val protobufListReference = getInstruction<ReferenceInstruction>(protobufListIndex).reference
            val protobufListClass = (protobufListReference as MethodReference).returnType

            // Hooks all instructions that load or save protobufList.
            for (index in implementation!!.instructions.size - 1 downTo 0) {
                val instruction = getInstruction(index)

                if (instruction.opcode != Opcode.IGET_OBJECT
                            && instruction.opcode != Opcode.IPUT_OBJECT)
                    continue

                val fieldReference = instruction.getReference<FieldReference>()
                if (fieldReference?.definingClass != STREAMING_DATA_OUTER_CLASS)
                    continue
                if (fieldReference.type != protobufListClass)
                    continue

                val insertRegister = getInstruction<TwoRegisterInstruction>(index).registerA
                val insertIndex =
                    if (instruction.opcode == Opcode.IPUT_OBJECT)
                        index
                    else
                        index + 1

                addInstruction(
                    insertIndex,
                    "invoke-static { v$insertRegister }, " +
                            INTEGRATIONS_METHOD_CALL
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MISC_EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_FORMAT_STREAM_DATA"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
