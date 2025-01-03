package app.revanced.patches.shared.spoof.streamingdata

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatchBuilder
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.extension.Constants.SPOOF_PATH
import app.revanced.patches.shared.formatStreamModelConstructorFingerprint
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
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

fun baseSpoofStreamingDataPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    name = "Spoof streaming data",
    description = "Adds options to spoof the streaming data to allow playback."
) {
    block()

    execute {
        // region Block /initplayback requests to fall back to /get_watch requests.

        buildInitPlaybackRequestFingerprint.matchOrThrow().let {
            it.method.apply {
                val moveUriStringIndex = it.patternMatch!!.startIndex
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(moveUriStringIndex).registerA

                addInstructions(
                    moveUriStringIndex + 1,
                    """
                        invoke-static { v$targetRegister }, $EXTENSION_CLASS_DESCRIPTOR->blockInitPlaybackRequest(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """,
                )
            }
        }

        // endregion

        // region Block /get_watch requests to fall back to /player requests.

        buildPlayerRequestURIFingerprint.methodOrThrow().apply {
            val invokeToStringIndex = indexOfToStringInstruction(this)
            val uriRegister =
                getInstruction<FiveRegisterInstruction>(invokeToStringIndex).registerC

            addInstructions(
                invokeToStringIndex,
                """
                    invoke-static { v$uriRegister }, $EXTENSION_CLASS_DESCRIPTOR->blockGetWatchRequest(Landroid/net/Uri;)Landroid/net/Uri;
                    move-result-object v$uriRegister
                    """,
            )
        }

        // endregion

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

        val approxDurationMsReference = formatStreamModelConstructorFingerprint.matchOrThrow().let {
            with (it.method) {
                getInstruction<ReferenceInstruction>(it.patternMatch!!.startIndex).reference
            }
        }

        val streamingDataFormatsReference = with(videoStreamingDataConstructorFingerprint.methodOrThrow(videoStreamingDataToStringFingerprint)) {
            val getFormatsFieldIndex = indexOfGetFormatsFieldInstruction(this)
            val longMaxValueIndex = indexOfLongMaxValueInstruction(this, getFormatsFieldIndex)
            val longMaxValueRegister = getInstruction<OneRegisterInstruction>(longMaxValueIndex).registerA
            val videoIdIndex =
                indexOfFirstInstructionOrThrow(longMaxValueIndex) {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IGET_OBJECT &&
                            reference?.type == "Ljava/lang/String;" &&
                            reference.definingClass == definingClass
                }

            val definingClassRegister =
                getInstruction<TwoRegisterInstruction>(videoIdIndex).registerB
            val videoIdReference =
                getInstruction<ReferenceInstruction>(videoIdIndex).reference

            addInstructions(
                longMaxValueIndex + 1, """
                    # Get video id.
                    iget-object v$longMaxValueRegister, v$definingClassRegister, $videoIdReference
                    
                    # Override approxDurationMs.
                    invoke-static { v$longMaxValueRegister }, $EXTENSION_CLASS_DESCRIPTOR->getApproxDurationMs(Ljava/lang/String;)J
                    move-result-wide v$longMaxValueRegister
                    """
            )
            removeInstruction(longMaxValueIndex)

            getInstruction<ReferenceInstruction>(getFormatsFieldIndex).reference
        }

        createStreamingDataFingerprint.matchOrThrow(createStreamingDataParentFingerprint)
            .let { result ->
                result.method.apply {
                    val setStreamDataMethodName = "patch_setStreamingData"
                    val calcApproxDurationMsMethodName = "patch_calcApproxDurationMs"
                    val resultClassDef = result.classDef
                    val resultMethodType = resultClassDef.type
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
                                    invoke-static { v2 }, $EXTENSION_CLASS_DESCRIPTOR->getStreamingData(Ljava/lang/String;)Ljava/nio/ByteBuffer;
                                    move-result-object v3
                                    
                                    if-eqz v3, :disabled
                                    
                                    # Parse streaming data.
                                    sget-object v4, $playerProtoClass->a:$playerProtoClass
                                    invoke-static { v4, v3 }, $protobufClass->parseFrom(${protobufClass}Ljava/nio/ByteBuffer;)$protobufClass
                                    move-result-object v5
                                    check-cast v5, $playerProtoClass
                                    
                                    iget-object v6, v5, $getStreamingDataField
                                    if-eqz v6, :disabled
                                    
                                    # Caculate approxDurationMs.
                                    invoke-direct { p0, v2 }, $resultMethodType->$calcApproxDurationMsMethodName(Ljava/lang/String;)V
                                    
                                    # Set spoofed streaming data.
                                    iput-object v6, p0, $setStreamingDataField
                                    
                                    :disabled
                                    return-void
                                    """,
                            )
                        },
                    )

                    resultClassDef.methods.add(
                        ImmutableMethod(
                            resultMethodType,
                            calcApproxDurationMsMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    "Ljava/lang/String;",
                                    annotations,
                                    "videoId"
                                )
                            ),
                            "V",
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(12),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                    # Get video format list.
                                    iget-object v0, p0, $setStreamingDataField
                                    iget-object v0, v0, $streamingDataFormatsReference
                                    invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                                    move-result-object v0
                                    
                                    # Initialize approxDurationMs field.
                                    const-wide v1, 0x7fffffffffffffffL
                                    
                                    :loop
                                    # Loop over all video formats to get the approxDurationMs
                                    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
                                    move-result v3
                                    const-wide/16 v4, 0x0
                                    
                                    if-eqz v3, :exit
                                    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                                    move-result-object v3
                                    check-cast v3, ${(approxDurationMsReference as FieldReference).definingClass}
                                    
                                    # Get approxDurationMs from format
                                    iget-wide v6, v3, $approxDurationMsReference
                                    
                                    # Compare with zero to make sure approxDurationMs is not negative
                                    cmp-long v8, v6, v4
                                    if-lez v8, :loop
                                    
                                    # Only use the min value of approxDurationMs
                                    invoke-static {v1, v2, v6, v7}, Ljava/lang/Math;->min(JJ)J
                                    move-result-wide v1
                                    goto :loop
                                    
                                    :exit
                                    # Save approxDurationMs to integrations
                                    invoke-static { p1, v1, v2 }, $EXTENSION_CLASS_DESCRIPTOR->setApproxDurationMs(Ljava/lang/String;J)V
                                    
                                    return-void
                                    """,
                            )
                        },
                    )
                }
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

        // region Set DroidGuard poToken.

        poTokenToStringFingerprint.mutableClassOrThrow().let {
            val poTokenClass = it.fields.find { field ->
                field.accessFlags == AccessFlags.PRIVATE.value && field.type.startsWith("L")
            }!!.type

            findMethodOrThrow(poTokenClass) {
                name == "<init>" &&
                        parameters == listOf("[B")
            }.addInstruction(
                1,
                "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->setDroidGuardPoToken([B)V"
            )
        }

        // endregion

        executeBlock()

    }
}
