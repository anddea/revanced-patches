package app.revanced.patches.youtube.utils.fix.streamingdata

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.compatibility.Constants
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.BuildBrowseRequestFingerprint
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.BuildInitPlaybackRequestFingerprint
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.BuildMediaDataSourceFingerprint
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.BuildPlayerRequestURIFingerprint
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.CreateStreamingDataFingerprint
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.NerdsStatsVideoFormatBuilderFingerprint
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.ProtobufClassParseByteBufferFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.findOpcodeIndicesReversed
import app.revanced.util.getReference
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

object SpoofStreamingDataPatch : BaseBytecodePatch(
    name = "Spoof streaming data",
    description = "Adds options to spoof the streaming data to allow video playback.",
    dependencies = setOf(
        SettingsPatch::class,
        SpoofUserAgentPatch::class,
    ),
    compatiblePackages = Constants.COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        BuildBrowseRequestFingerprint,
        BuildInitPlaybackRequestFingerprint,
        BuildMediaDataSourceFingerprint,
        BuildPlayerRequestURIFingerprint,
        CreateStreamingDataFingerprint,
        ProtobufClassParseByteBufferFingerprint,

        // Nerds stats video format.
        NerdsStatsVideoFormatBuilderFingerprint,
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/SpoofStreamingDataPatch;"

    override fun execute(context: BytecodeContext) {

        // region Block /get_watch requests to fall back to /player requests.

        BuildPlayerRequestURIFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val invokeToStringIndex =
                    BuildPlayerRequestURIFingerprint.indexOfToStringInstruction(this)
                val uriRegister =
                    getInstruction<FiveRegisterInstruction>(invokeToStringIndex).registerC

                addInstructions(
                    invokeToStringIndex,
                    """
                        invoke-static { v$uriRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->blockGetWatchRequest(Landroid/net/Uri;)Landroid/net/Uri;
                        move-result-object v$uriRegister
                        """,
                )
            }
        }

        // endregion

        // region Block /initplayback requests to fall back to /get_watch requests.

        BuildInitPlaybackRequestFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val moveUriStringIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(moveUriStringIndex).registerA

                addInstructions(
                    moveUriStringIndex + 1,
                    """
                        invoke-static { v$targetRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->blockInitPlaybackRequest(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """,
                )
            }
        }

        // endregion

        // region Fetch replacement streams.

        BuildBrowseRequestFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                val newRequestBuilderIndex =
                    BuildBrowseRequestFingerprint.indexOfNewUrlRequestBuilderInstruction(this)
                val urlRegister =
                    getInstruction<FiveRegisterInstruction>(newRequestBuilderIndex).registerD

                val entrySetIndex = BuildBrowseRequestFingerprint.indexOfEntrySetInstruction(this)
                val mapRegister = if (entrySetIndex < 0)
                    urlRegister + 1
                else
                    getInstruction<FiveRegisterInstruction>(entrySetIndex).registerC

                var smaliInstructions =
                    "invoke-static { v$urlRegister, v$mapRegister }, " +
                            "$INTEGRATIONS_CLASS_DESCRIPTOR->" +
                            "fetchStreams(Ljava/lang/String;Ljava/util/Map;)V"

                if (entrySetIndex < 0) smaliInstructions = """
                        move-object/from16 v$mapRegister, p1
                        
                        """ + smaliInstructions

                // Copy request headers for streaming data fetch.
                addInstructions(newRequestBuilderIndex + 2, smaliInstructions)
            }
        }

        // endregion

        // region Replace the streaming data.

        CreateStreamingDataFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                val setStreamingDataIndex = result.scanResult.patternScanResult!!.startIndex
                val setStreamingDataField =
                    getInstruction(setStreamingDataIndex).getReference<FieldReference>().toString()

                val playerProtoClass =
                    getInstruction(setStreamingDataIndex + 1).getReference<FieldReference>()!!.definingClass
                val protobufClass =
                    ProtobufClassParseByteBufferFingerprint.resultOrThrow().mutableMethod.definingClass

                val getStreamingDataField = getInstructions().find { instruction ->
                    instruction.opcode == Opcode.IGET_OBJECT &&
                            instruction.getReference<FieldReference>()?.definingClass == playerProtoClass
                }?.getReference<FieldReference>()
                    ?: throw PatchException("Could not find getStreamingDataField")

                val videoDetailsIndex = result.scanResult.patternScanResult!!.endIndex
                val videoDetailsClass =
                    getInstruction(videoDetailsIndex).getReference<FieldReference>()!!.type

                val insertIndex = videoDetailsIndex + 1
                val videoDetailsRegister =
                    getInstruction<TwoRegisterInstruction>(videoDetailsIndex).registerA

                val overrideRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                val freeRegister = implementation!!.registerCount - parameters.size - 2

                addInstructionsWithLabels(
                    insertIndex,
                    """
                        invoke-static { }, $INTEGRATIONS_CLASS_DESCRIPTOR->isSpoofingEnabled()Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :disabled

                        # Get video id.
                        # From YouTube 17.34.36 to YouTube 19.16.39, the field names and field types are the same.
                        iget-object v$freeRegister, v$videoDetailsRegister, $videoDetailsClass->c:Ljava/lang/String;
                        if-eqz v$freeRegister, :disabled

                        # Get streaming data.
                        invoke-static { v$freeRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getStreamingData(Ljava/lang/String;)Ljava/nio/ByteBuffer;
                        move-result-object v$freeRegister
                        if-eqz v$freeRegister, :disabled

                        # Parse streaming data.
                        sget-object v$overrideRegister, $playerProtoClass->a:$playerProtoClass
                        invoke-static { v$overrideRegister, v$freeRegister }, $protobufClass->parseFrom(${protobufClass}Ljava/nio/ByteBuffer;)$protobufClass
                        move-result-object v$freeRegister
                        check-cast v$freeRegister, $playerProtoClass

                        # Set streaming data.
                        iget-object v$freeRegister, v$freeRegister, $getStreamingDataField
                        if-eqz v0, :disabled
                        iput-object v$freeRegister, p0, $setStreamingDataField

                        """,
                    ExternalLabel("disabled", getInstruction(insertIndex))
                )
            }
        }

        // endregion

        // region Remove /videoplayback request body to fix playback.
        // This is needed when using iOS client as streaming data source.

        BuildMediaDataSourceFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getInstructions().lastIndex

                addInstructions(
                    targetIndex,
                    """
                        # Field a: Stream uri.
                        # Field c: Http method.
                        # Field d: Post data.
                        # From YouTube 17.34.36 to YouTube 19.16.39, the field names and field types are the same.
                        move-object/from16 v0, p0
                        iget-object v1, v0, $definingClass->a:Landroid/net/Uri;
                        iget v2, v0, $definingClass->c:I
                        iget-object v3, v0, $definingClass->d:[B
                        invoke-static { v1, v2, v3 }, $INTEGRATIONS_CLASS_DESCRIPTOR->removeVideoPlaybackPostBody(Landroid/net/Uri;I[B)[B
                        move-result-object v1
                        iput-object v1, v0, $definingClass->d:[B
                        """,
                )
            }
        }

        // endregion

        // region Append spoof info.

        NerdsStatsVideoFormatBuilderFingerprint.resultOrThrow().mutableMethod.apply {
            findOpcodeIndicesReversed(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->appendSpoofedClient(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """
                )
            }
        }

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: SPOOF_STREAMING_DATA"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
