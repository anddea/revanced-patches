package app.revanced.patches.youtube.utils.fix.formatstream

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.compatibility.Constants
import app.revanced.patches.youtube.utils.fix.formatstream.fingerprints.EndpointUrlBuilderFingerprint
import app.revanced.patches.youtube.utils.fix.formatstream.fingerprints.FormatStreamModelConstructorFingerprint
import app.revanced.patches.youtube.utils.fix.formatstream.fingerprints.PlaybackStartConstructorFingerprint
import app.revanced.patches.youtube.utils.fix.formatstream.fingerprints.PlaybackStartParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch
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
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Suppress("unused")
object SpoofFormatStreamDataPatch : BaseBytecodePatch(
    name = "Spoof format stream data",
    description = "Adds options to spoof format stream data to prevent playback issues.",
    dependencies = setOf(
        SettingsPatch::class,
        VideoInformationPatch::class,
    ),
    compatiblePackages = Constants.COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        EndpointUrlBuilderFingerprint,
        FormatStreamModelConstructorFingerprint,
        PlaybackStartParentFingerprint
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

        // region set field name

        hookMethod = context.findClass(INTEGRATIONS_CLASS_DESCRIPTOR)!!
            .mutableClass.methods.find { method -> method.name == INTEGRATIONS_METHOD_DESCRIPTOR }
            ?: throw PatchException("SpoofFormatStreamDataPatch not found")

        FormatStreamModelConstructorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {

                // Find the field name that will be used for reflection.
                val urlIndex = it.scanResult.patternScanResult!!.startIndex
                val itagIndex = getTargetIndex(urlIndex + 1, Opcode.IGET)

                replaceFieldName(urlIndex, "replaceMeWithUrlFieldName")
                replaceFieldName(itagIndex, "replaceMeWithITagFieldName")
            }
        }

        // endregion

        // region set protobufList reference

        lateinit var nonDashProtobufListReference: Reference
        lateinit var dashProtobufListReference: Reference

        val streamingDataOutClassConstructorMethod = context.findClass(STREAMING_DATA_OUTER_CLASS)!!
            .mutableClass.methods.find { method -> method.name == "<init>" }
            ?: throw PatchException("StreamingDataOutClass not found")

        streamingDataOutClassConstructorMethod.apply {
            val protobufListIndex = indexOfFirstInstruction {
                opcode == Opcode.INVOKE_STATIC
                        && getReference<MethodReference>()?.definingClass == STREAMING_DATA_OUTER_CLASS
                        && getReference<MethodReference>()?.name == "emptyProtobufList"
            }
            if (protobufListIndex <= 0)
                throw PatchException("ProtobufList index not found")

            val protobufListReference = getInstruction<ReferenceInstruction>(protobufListIndex).reference
            val protobufListClass = (protobufListReference as MethodReference).returnType

            val protobufListCalls = implementation!!.instructions.withIndex()
                .filter { instruction ->
                    ((instruction.value as? ReferenceInstruction)?.reference as? FieldReference)?.type == protobufListClass
                }

            nonDashProtobufListReference =
                getInstruction<ReferenceInstruction>(protobufListCalls.elementAt(0).index).reference
            dashProtobufListReference =
                getInstruction<ReferenceInstruction>(protobufListCalls.elementAt(1).index).reference
        }

        // endregion

        // region hook stream data

        PlaybackStartConstructorFingerprint.resolve(
            context,
            PlaybackStartParentFingerprint.resultOrThrow().classDef
        )
        PlaybackStartConstructorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val streamingDataOuterClassIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val streamingDataOuterClassReference = getInstruction<ReferenceInstruction>(streamingDataOuterClassIndex).reference
                if (!streamingDataOuterClassReference.toString().endsWith(STREAMING_DATA_OUTER_CLASS))
                    throw PatchException("Type does not match: $streamingDataOuterClassReference")

                val insertIndex = streamingDataOuterClassIndex + 1
                val streamingDataOuterClassRegister = getInstruction<TwoRegisterInstruction>(streamingDataOuterClassIndex).registerA
                val freeRegister = implementation!!.registerCount - parameters.size - 2

                addInstructionsWithLabels(
                    insertIndex,
                    """
                        if-eqz v$streamingDataOuterClassRegister, :ignore
                        iget-object v$freeRegister, v$streamingDataOuterClassRegister, $nonDashProtobufListReference
                        invoke-static { v$freeRegister }, $INTEGRATIONS_METHOD_CALL
                        iget-object v$freeRegister, v$streamingDataOuterClassRegister, $dashProtobufListReference
                        invoke-static { v$freeRegister }, $INTEGRATIONS_METHOD_CALL
                    """, ExternalLabel("ignore", getInstruction(insertIndex))
                )
            }
        }

        // endregion

        // region hook endpoint url

        EndpointUrlBuilderFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val uriIndex = indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_VIRTUAL
                            && getReference<MethodReference>()?.definingClass == "Landroid/net/Uri;"
                            && getReference<MethodReference>()?.name == "toString"
                }
                val uriStringIndex = getTargetIndex(uriIndex, Opcode.IPUT_OBJECT)
                val uriStringReference = getInstruction<ReferenceInstruction>(uriStringIndex).reference

                it.mutableClass.methods.find { method ->
                    method.parameters == listOf("Lcom/google/protobuf/MessageLite;")
                            && method.returnType == "V"
                }?.addInstructions(
                    0,
                    """
                        iget-object v0, p0, $uriStringReference
                        invoke-static { v0 }, $INTEGRATIONS_CLASS_DESCRIPTOR->newEndpointUrlResponse(Ljava/lang/String;)V
                    """
                ) ?: throw PatchException("PlaybackStart method not found")
            }
        }

        // endregion

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
