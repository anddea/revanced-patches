package app.revanced.patches.shared.spoof.streamingdata

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatchBuilder
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.blockrequest.blockRequestPatch
import app.revanced.patches.shared.extension.Constants.SPOOF_PATH
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

const val EXTENSION_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/SpoofStreamingDataPatch;"

// In YouTube 17.34.36, this class is obfuscated.
const val STREAMING_DATA_INTERFACE =
    "Lcom/google/protos/youtube/api/innertube/StreamingDataOuterClass${'$'}StreamingData;"

fun baseSpoofStreamingDataPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    name = "Spoof streaming data",
    description = "Adds options to spoof the streaming data to allow playback."
) {
    dependsOn(blockRequestPatch)

    block()

    execute {
        // region Get replacement streams at player requests.

        buildRequestFingerprint.methodOrThrow().apply {
            val newRequestBuilderIndex = indexOfNewUrlRequestBuilderInstruction(this)
            val urlRegister =
                getInstruction<FiveRegisterInstruction>(newRequestBuilderIndex).registerD

            val entrySetIndex = indexOfEntrySetInstruction(this)
            val mapRegister = if (entrySetIndex < 0)
                urlRegister + 1
            else
                getInstruction<FiveRegisterInstruction>(entrySetIndex).registerC

            var smaliInstructions =
                "invoke-static { v$urlRegister, v$mapRegister }, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->" +
                        "fetchStreams(Ljava/lang/String;Ljava/util/Map;)V"

            if (entrySetIndex < 0) smaliInstructions = """
                        move-object/from16 v$mapRegister, p1
                        
                        """ + smaliInstructions

            // Copy request headers for streaming data fetch.
            addInstructions(newRequestBuilderIndex + 2, smaliInstructions)
        }

        // endregion

        // region Replace the streaming data.

        createStreamingDataFingerprint.matchOrThrow(createStreamingDataParentFingerprint)
            .let { result ->
                result.method.apply {
                    val setStreamDataMethodName = "patch_setStreamingData"
                    val resultMethodType = result.classDef.type
                    val setStreamingDataIndex = result.patternMatch!!.startIndex
                    val setStreamingDataField =
                        getInstruction(setStreamingDataIndex).getReference<FieldReference>()
                            .toString()

                    val playerProtoClass =
                        getInstruction(setStreamingDataIndex + 1).getReference<FieldReference>()!!.definingClass
                    val protobufClass =
                        protobufClassParseByteBufferFingerprint.definingClassOrThrow()

                    val getStreamingDataField = instructions.find { instruction ->
                        instruction.opcode == Opcode.IGET_OBJECT &&
                                instruction.getReference<FieldReference>()?.definingClass == playerProtoClass
                    }?.getReference<FieldReference>()
                        ?: throw PatchException("Could not find getStreamingDataField")

                    val videoDetailsIndex = result.patternMatch!!.endIndex
                    val videoDetailsRegister =
                        getInstruction<TwoRegisterInstruction>(videoDetailsIndex).registerA
                    val videoDetailsClass =
                        getInstruction(videoDetailsIndex).getReference<FieldReference>()!!.type

                    addInstruction(
                        videoDetailsIndex + 1,
                        "invoke-direct { p0, v$videoDetailsRegister }, " +
                                "$resultMethodType->$setStreamDataMethodName($videoDetailsClass)V",
                    )

                    result.classDef.methods.add(
                        ImmutableMethod(
                            resultMethodType,
                            setStreamDataMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    videoDetailsClass,
                                    annotations,
                                    "videoDetails"
                                )
                            ),
                            "V",
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(9),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->isSpoofingEnabled()Z
                                move-result v0
                                if-eqz v0, :disabled
                                
                                # Get video id.
                                iget-object v2, p1, $videoDetailsClass->c:Ljava/lang/String;
                                if-eqz v2, :disabled
                                
                                # Get streaming data.
                                iget-object v6, p0, $setStreamingDataField
                                invoke-static { v2, v6 }, $EXTENSION_CLASS_DESCRIPTOR->getStreamingData(Ljava/lang/String;$STREAMING_DATA_INTERFACE)Ljava/nio/ByteBuffer;
                                move-result-object v3
                                if-eqz v3, :disabled
                                
                                # Parse streaming data.
                                sget-object v4, $playerProtoClass->a:$playerProtoClass
                                invoke-static { v4, v3 }, $protobufClass->parseFrom(${protobufClass}Ljava/nio/ByteBuffer;)$protobufClass
                                move-result-object v5
                                check-cast v5, $playerProtoClass
                                
                                # Set streaming data.
                                iget-object v6, v5, $getStreamingDataField
                                if-eqz v6, :disabled
                                iput-object v6, p0, $setStreamingDataField
                                
                                :disabled
                                return-void
                                """,
                            )
                        },
                    )
                }
            }

        videoStreamingDataConstructorFingerprint.methodOrThrow(videoStreamingDataToStringFingerprint)
            .apply {
                val formatStreamModelInitIndex = indexOfFormatStreamModelInitInstruction(this)
                val getVideoIdIndex =
                    indexOfFirstInstructionReversedOrThrow(formatStreamModelInitIndex) {
                        val reference = getReference<FieldReference>()
                        opcode == Opcode.IGET_OBJECT &&
                                reference?.type == "Ljava/lang/String;" &&
                                reference.definingClass == definingClass
                    }
                val getVideoIdReference =
                    getInstruction<ReferenceInstruction>(getVideoIdIndex).reference
                val insertIndex = indexOfFirstInstructionReversedOrThrow(getVideoIdIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.definingClass == STREAMING_DATA_INTERFACE
                }

                val (freeRegister, streamingDataRegister) = with(
                    getInstruction<TwoRegisterInstruction>(
                        insertIndex
                    )
                ) {
                    Pair(registerA, registerB)
                }
                val definingClassRegister =
                    getInstruction<TwoRegisterInstruction>(getVideoIdIndex).registerB
                val insertReference = getInstruction<ReferenceInstruction>(insertIndex).reference

                replaceInstruction(
                    insertIndex,
                    "iget-object v$freeRegister, v$freeRegister, $insertReference"
                )
                addInstructions(
                    insertIndex, """
                    iget-object v$freeRegister, v$definingClassRegister, $getVideoIdReference
                    invoke-static { v$freeRegister, v$streamingDataRegister }, $EXTENSION_CLASS_DESCRIPTOR->getOriginalStreamingData(Ljava/lang/String;$STREAMING_DATA_INTERFACE)$STREAMING_DATA_INTERFACE
                    move-result-object v$freeRegister
                    """
                )
            }

        // endregion

        // region Remove /videoplayback request body to fix playback.
        // This is needed when using iOS client as streaming data source.

        buildMediaDataSourceFingerprint.methodOrThrow().apply {
            val targetIndex = instructions.lastIndex

            addInstructions(
                targetIndex,
                """
                    # Field a: Stream uri.
                    # Field c: Http method.
                    # Field d: Post data.
                    move-object/from16 v0, p0
                    iget-object v1, v0, $definingClass->a:Landroid/net/Uri;
                    iget v2, v0, $definingClass->c:I
                    iget-object v3, v0, $definingClass->d:[B
                    invoke-static { v1, v2, v3 }, $EXTENSION_CLASS_DESCRIPTOR->removeVideoPlaybackPostBody(Landroid/net/Uri;I[B)[B
                    move-result-object v1
                    iput-object v1, v0, $definingClass->d:[B
                    """,
            )
        }

        // endregion

        // region Append spoof info.

        nerdsStatsVideoFormatBuilderFingerprint.methodOrThrow().apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->appendSpoofedClient(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """
                )
            }
        }

        // endregion

        // region Fix iOS livestream current time.

        hlsCurrentTimeFingerprint.injectLiteralInstructionBooleanCall(
            HLS_CURRENT_TIME_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->fixHLSCurrentTime(Z)Z"
        )

        // endregion

        executeBlock()

    }
}
