@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package app.revanced.patches.music.utils.fix.streamingdata

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.music.utils.extension.Constants.SPOOF_PATH
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/SpoofVideoStreamsPatch;"
private const val EXTENSION_STREAMING_DATA_INTERFACE =
    "$SPOOF_PATH/SpoofVideoStreamsPatch${'$'}StreamingDataMessage;"

context(BytecodePatchContext)
internal fun patchSpoofVideoStreams() {

    buildRequestFingerprint.methodOrThrow(buildRequestParentFingerprint).apply {
        val newRequestBuilderIndex = indexOfNewUrlRequestBuilderInstruction(this)
        val urlRegister =
            getInstruction<FiveRegisterInstruction>(newRequestBuilderIndex).registerD

        addInstructions(
            newRequestBuilderIndex,
            "invoke-static { v$urlRegister, p1 }, $EXTENSION_CLASS_DESCRIPTOR->fetchStreams(Ljava/lang/String;Ljava/util/Map;)V"
        )
    }

    // region Replace the streaming data.

    createStreamingDataFingerprint.matchOrThrow(createStreamingDataParentFingerprint)
        .let { result ->
            result.method.apply {
                val parseResponseProtoMethodName = "parseFrom"

                val setStreamDataMethodName = "patch_setStreamingData"
                val resultClassDef = result.classDef
                val resultMethodType = resultClassDef.type
                val setStreamingDataIndex = result.patternMatch!!.startIndex
                val setStreamingDataRegister =
                    getInstruction<TwoRegisterInstruction>(setStreamingDataIndex).registerA

                val playerProtoClass =
                    getInstruction(setStreamingDataIndex + 1).getReference<FieldReference>()!!.definingClass
                val protobufClass =
                    protobufClassParseByteBufferFingerprint.definingClassOrThrow()

                val getStreamingDataField = instructions.find { instruction ->
                    instruction.opcode == Opcode.IGET_OBJECT &&
                            instruction.getReference<FieldReference>()?.definingClass == playerProtoClass
                }?.getReference<FieldReference>()
                    ?: throw PatchException("Could not find getStreamingDataField")

                val getVideoDetailsIndex = setStreamingDataIndex + 1
                val getVideoDetailsFallbackIndex = setStreamingDataIndex + 3
                val getVideoDetailsField =
                    getInstruction(getVideoDetailsIndex).getReference<FieldReference>()!!
                val getVideoDetailsFallbackField =
                    getInstruction(getVideoDetailsFallbackIndex).getReference<FieldReference>()!!
                val freeRegister = implementation!!.registerCount - parameters.size - 2

                addInstructionsAtControlFlowLabel(
                    setStreamingDataIndex, """
                        iget-object v$freeRegister, p1, $getVideoDetailsField
                        if-nez v$freeRegister, :ignore
                        sget-object v$freeRegister, $getVideoDetailsFallbackField
                        :ignore
                        iget-object v$freeRegister, v$freeRegister, ${getVideoDetailsField.type}->c:Ljava/lang/String;
                        invoke-direct { p0, v$setStreamingDataRegister, v$freeRegister }, $resultMethodType->$setStreamDataMethodName(${STREAMING_DATA_OUTER_CLASS}Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                        move-result-object v$setStreamingDataRegister
                        """
                )

                result.classDef.methods.filter { method ->
                    MethodUtil.isConstructor(method)
                }.forEach { method ->
                    method.addInstruction(
                        1,
                        "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->initialize($EXTENSION_STREAMING_DATA_INTERFACE)V"
                    )
                }

                resultClassDef.interfaces.add(EXTENSION_STREAMING_DATA_INTERFACE)

                result.classDef.methods.add(
                    ImmutableMethod(
                        resultMethodType,
                        parseResponseProtoMethodName,
                        listOf(
                            ImmutableMethodParameter(
                                "Ljava/nio/ByteBuffer;",
                                annotations,
                                "responseProto"
                            )
                        ),
                        STREAMING_DATA_OUTER_CLASS,
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(5),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                # Check buffer is not null.
                                if-nez p1, :parse
                                const/4 v0, 0x0
                                return-object v0
                                :parse

                                # Parse streaming data.
                                sget-object v0, $playerProtoClass->a:$playerProtoClass
                                invoke-static { v0, p1 }, $protobufClass->parseFrom(${protobufClass}Ljava/nio/ByteBuffer;)$protobufClass
                                move-result-object v0
                                check-cast v0, $playerProtoClass
                                iget-object v0, v0, $getStreamingDataField
                                return-object v0
                                """,
                        )
                    },
                )

                result.classDef.methods.add(
                    ImmutableMethod(
                        resultMethodType,
                        setStreamDataMethodName,
                        listOf(
                            ImmutableMethodParameter(
                                STREAMING_DATA_OUTER_CLASS,
                                annotations,
                                "streamingDataOuterClass"
                            ),
                            ImmutableMethodParameter(
                                "Ljava/lang/String;",
                                annotations,
                                "videoId"
                            )
                        ),
                        STREAMING_DATA_OUTER_CLASS,
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(5),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                # Get streaming data.
                                invoke-static { p2 }, $EXTENSION_CLASS_DESCRIPTOR->getStreamingData(Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                                move-result-object v0
                                if-eqz v0, :ignore                                    
                                return-object v0
                                :ignore
                                return-object p1
                                """,
                        )
                    },
                )
            }
        }

    // endregion

    hlsCurrentTimeFingerprint.injectLiteralInstructionBooleanCall(
        HLS_CURRENT_TIME_FEATURE_FLAG,
        "$EXTENSION_CLASS_DESCRIPTOR->fixHLSCurrentTime(Z)Z"
    )

    // region Skip response encryption in OnesiePlayerRequest

    if (onesieEncryptionFeatureFlagFingerprint.resolvable()) {
        onesieEncryptionFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
            ONESIE_ENCRYPTION_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->skipResponseEncryption(Z)Z"
        )

        if (playbackStartDescriptorFeatureFlagFingerprint.resolvable()) {
            playbackStartDescriptorFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                PLAYBACK_START_CHECK_ENDPOINT_USED_FEATURE_FLAG,
                "$EXTENSION_CLASS_DESCRIPTOR->usePlaybackStartFeatureFlag(Z)Z"
            )

            // In 20.14 the flag was merged with 20.03 start playback flag.
            if (onesieEncryptionAlternativeFeatureFlagFingerprint.resolvable()) {
                onesieEncryptionAlternativeFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                    ONESIE_ENCRYPTION_ALTERNATIVE_FEATURE_FLAG,
                    "$EXTENSION_CLASS_DESCRIPTOR->skipResponseEncryption(Z)Z"
                )
            }
        }
    }

    // endregion

}
