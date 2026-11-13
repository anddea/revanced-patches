/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.downloads.native

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.cloneParameters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.registersUsed
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.shared.misc.fix.proto.parseByteArrayWithRegistryMethodRef
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.fix.streamingdata.spoofStreamingDataPatch
import app.morphe.patches.youtube.utils.proto.elementProtoParserHookPatch
import app.morphe.patches.youtube.utils.proto.hookElement
import app.morphe.patches.youtube.utils.commandrouter.commandRouterHookPatch
import app.morphe.patches.youtube.utils.playservice.is_20_21_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch

internal const val EXTENSION_IN_APP_DOWNLOADS_CLASS = "Lapp/morphe/extension/youtube/patches/InAppDownloadsPatch;"

private const val EXTENSION_SPOOF_CLASS = "Lapp/morphe/extension/shared/spoof/SpoofVideoStreamsPatch;"

private const val EXTENSION_SHORTS_DOWNLOAD_BUTTON =
    "Lapp/morphe/extension/youtube/videoplayer/ShortsDownloadButton;"

private const val EXTENSION_EXPORT_DOWNLOAD_BUTTON =
    "Lapp/morphe/extension/youtube/videoplayer/ExportDownloadButton;"

private const val EXTENSION_BUTTON = "Lapp/morphe/extension/youtube/videoplayer/ExternalDownloadButton;"

private const val GET_GENERATED_REGISTRY =
    "Lcom/google/protobuf/ExtensionRegistryLite;->getGeneratedRegistry()Lcom/google/protobuf/ExtensionRegistryLite;"

/**
 * @return The default instance field of a proto message, which the message is parsed with.
 */
private fun protoDefaultInstance(messageType: String) =
    Fingerprint(definingClass = messageType, name = "<clinit>", filters = listOf(fieldAccess(definingClass = messageType, type = messageType, opcode = com.android.tools.smali.dexlib2.Opcode.SPUT_OBJECT))).instructionMatches.first().fieldReference

/** Morphe native offline downloads and its playback, quality and settings fixes. */
internal val nativeDownloadsPatch = patch {
    dependsOn(sharedExtensionPatch, settingsPatch, spoofStreamingDataPatch, fixProtoLibraryPatch,
        elementProtoParserHookPatch, commandRouterHookPatch, versionCheckPatch)
    execute {
        if (!is_20_21_or_greater) {
            app.morphe.util.Utils.printWarn("Native downloads require YouTube 20.21 or later; using the configured downloader.")
            return@execute
        }
        Fingerprint(
            definingClass = EXTENSION_IN_APP_DOWNLOADS_CLASS,
            name = "isPatchIncluded",
            returnType = "Z"
        ).method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")

        // Add the Download button to the playlist page headers, which only Premium accounts get.
        hookElement("$EXTENSION_IN_APP_DOWNLOADS_CLASS->addPlaylistDownloadButton([B)[B")

        // region In-app downloads.

        // Save the video instead of asking the server for the download action, which answers with the Premium upsell.
        ResolveCommandFingerprint.method.apply {
            val commandType = parameterTypes.first().toString()

            addInstructionsWithLabels(
                0,
                """
                    # The parameter register can be higher than 15.
                    invoke-static/range { p1 .. p1 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->replaceDownloadActionCommand(Lcom/google/protobuf/MessageLite;)[B
                    move-result-object v0
                    if-eqz v0, :keep_command

                    # The endpoint is an extension, which is lost if parsed without the registry.
                    sget-object v1, ${protoDefaultInstance(commandType)}
                    invoke-static { }, $GET_GENERATED_REGISTRY
                    move-result-object v2
                    invoke-static { v1, v0, v2 }, ${parseByteArrayWithRegistryMethodRef.get()!!}
                    move-result-object v0
                    check-cast v0, $commandType
                    move-object/from16 p1, v0
                    :keep_command
                    nop
                """
            )
        }

        // Unlock the qualities of the video, and save the video instead of showing the Premium upsell.
        SaveOfflineVideoCallFingerprint.instructionMatches.first().getMethodCalled().apply {
            val offlineabilityType = parameterTypes[1].toString()

            addInstructionsWithLabels(
                0,
                """
                    # The parameter register can be higher than 15.
                    invoke-static/range { p2 .. p2 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->unlockOfflineability(Ljava/lang/Object;)[B
                    move-result-object v0
                    if-eqz v0, :keep_offlineability

                    sget-object v1, ${protoDefaultInstance(offlineabilityType)}
                    invoke-static { }, $GET_GENERATED_REGISTRY
                    move-result-object v2
                    invoke-static { v1, v0, v2 }, ${parseByteArrayWithRegistryMethodRef.get()!!}
                    move-result-object v0
                    check-cast v0, $offlineabilityType
                    move-object/from16 p2, v0
                    :keep_offlineability
                    nop
                """
            )
        }

        // Save the video instead of showing the Premium upsell of the offline upsell endpoint.
        OfflineUpsellResponseFingerprint.let {
            val responseType = it.instructionMatches.first().instruction.getReference<TypeReference>()!!.type
            val commandField = it.instructionMatches[2].fieldReference

            it.method.addInstructionsWithLabels(
                0,
                """
                    # The parameter registers can be higher than 15.
                    move-object/from16 v0, p0
                    iget-object v0, v0, $commandField
                    move-object/from16 v1, p1
                    invoke-static { v0, v1 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->replaceOfflineUpsell(Lcom/google/protobuf/MessageLite;Ljava/lang/Object;)[B
                    move-result-object v0
                    if-eqz v0, :keep_upsell_response

                    # The endpoint of the command is an extension, which is lost if parsed without the registry.
                    sget-object v1, ${protoDefaultInstance(responseType)}
                    invoke-static { }, $GET_GENERATED_REGISTRY
                    move-result-object v2
                    invoke-static { v1, v0, v2 }, ${parseByteArrayWithRegistryMethodRef.get()!!}
                    move-result-object v0
                    move-object/from16 p1, v0
                    :keep_upsell_response
                    nop
                """
            )
        }

        // Save the video with streams of another client, if the server declines to save the video.
        OfflinePlayerResponseFingerprint.let {
            val playerResponseModelType = it.method.returnType

            val fromBytesFingerprint = playerResponseModelFromBytesFingerprint(playerResponseModelType)
            val fromBytesMethod = fromBytesFingerprint.method
            val playerResponseType = fromBytesFingerprint.instructionMatches.last()
                .instruction.getReference<TypeReference>()!!.type
            val playerResponseGetter = playerResponseGetterFingerprint(
                fromBytesMethod.definingClass,
                playerResponseType
            ).method

            val helperMethod = ImmutableMethod(
                it.classDef.type,
                "patch_replaceDeclinedOfflinePlayerResponse",
                listOf(ImmutableMethodParameter(playerResponseModelType, null, null)),
                playerResponseModelType,
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(4),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0,
                    """
                        if-eqz p0, :keep_player_response
                        invoke-interface { p0 }, $playerResponseModelType->${playerResponseGetter.name}()$playerResponseType
                        move-result-object v0
                        invoke-static { v0 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->replaceDeclinedOfflinePlayerResponse(Ljava/lang/Object;)[B
                        move-result-object v0
                        if-eqz v0, :keep_player_response

                        # The playback stream override would replace the streams that can be saved.
                        const/4 v1, 0x1
                        invoke-static { v1 }, $EXTENSION_SPOOF_CLASS->setKeepStreamsOnThread(Z)V

                        # The response time, used for the expiration of the stream urls.
                        invoke-static { }, Ljava/lang/System;->currentTimeMillis()J
                        move-result-wide v1
                        invoke-static { v0, v1, v2 }, $fromBytesMethod
                        move-result-object v0

                        invoke-static { v0 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->onOfflinePlayerResponseReplaced(Ljava/lang/Object;)V
                        if-eqz v0, :keep_player_response
                        return-object v0

                        :keep_player_response
                        return-object p0
                    """
                )
            }
            it.classDef.methods.add(helperMethod)

            // Replace the player response before it is cached and returned.
            val cachePut = it.instructionMatches.last()
            val playerResponseModelRegister = cachePut.instruction.registersUsed[2]

            it.method.addInstructions(
                cachePut.index,
                """
                    invoke-static/range { v$playerResponseModelRegister .. v$playerResponseModelRegister }, $helperMethod
                    move-result-object v$playerResponseModelRegister
                """
            )
        }

        // Start saved videos played from Downloads at the position saved in Downloads.
        if (app.morphe.patches.youtube.utils.playservice.is_20_26_or_greater) {
            hookOfflineDownloadResume()
        }

        // Keep saved videos playable, if the server disables them when they are refreshed.
        UpdateOfflineVideoPolicyFingerprint.matchAll().forEach { match ->
            // The method has too few free registers.
            match.method.cloneParameters().apply {
                val offlineStateType = parameterTypes[2].toString()

                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static/range { p4 .. p4 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->replaceDisabledOfflineState(Lcom/google/protobuf/MessageLite;)[B
                        move-result-object v0
                        if-eqz v0, :keep_offline_state

                        sget-object v1, ${protoDefaultInstance(offlineStateType)}
                        invoke-static { }, $GET_GENERATED_REGISTRY
                        move-result-object v2
                        invoke-static { v1, v0, v2 }, ${parseByteArrayWithRegistryMethodRef.get()!!}
                        move-result-object v0
                        check-cast v0, $offlineStateType
                        move-object/from16 p4, v0
                        :keep_offline_state
                        nop
                    """
                )
            }
        }

        // Show the download settings in YouTube Settings > Background & downloads.
        // Without the offline setting category, the settings are hidden until a video is saved.
        val offlineSettingCategoryClass = OfflineSettingCategoryToStringFingerprint.classDef
        val offlineSettingCategoryType = offlineSettingCategoryClass.type
        val offlineSettingCategoryFromProto =
            offlineSettingCategoryFromProtoFingerprint(offlineSettingCategoryType).method

        val addOfflineSettingCategoryMethod = ImmutableMethod(
            offlineSettingCategoryType,
            "patch_addOfflineSettingCategory",
            listOf(ImmutableMethodParameter("Ljava/util/List;", null, null)),
            "Ljava/util/List;",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(3),
        ).toMutable().apply {
            addInstructionsWithLabels(
                0,
                """
                    const-class v0, $offlineSettingCategoryType
                    invoke-static { p0, v0 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->needsOfflineSettingCategory(Ljava/util/List;Ljava/lang/Class;)Z
                    move-result v0
                    if-eqz v0, :keep_setting_categories

                    # Category with the settings of the server all off.
                    sget-object v0, ${protoDefaultInstance(offlineSettingCategoryFromProto.parameterTypes.first().toString())}
                    invoke-static { v0 }, $offlineSettingCategoryFromProto
                    move-result-object v0

                    # The original list can be immutable.
                    new-instance v1, Ljava/util/ArrayList;
                    invoke-direct { v1, p0 }, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
                    invoke-virtual { v1, v0 }, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                    return-object v1

                    :keep_setting_categories
                    return-object p0
                """
            )
        }
        offlineSettingCategoryClass.methods.add(addOfflineSettingCategoryMethod)

        SettingCategoriesFingerprint.let {
            val returnMatch = it.instructionMatches.last()
            val categoriesRegister = returnMatch.instruction.registersUsed.first()

            it.method.addInstructionsAtControlFlowLabel(
                returnMatch.index,
                """
                    invoke-static { v$categoriesRegister }, $addOfflineSettingCategoryMethod
                    move-result-object v$categoriesRegister
                """
            )
        }

        OfflineSettingCategoryConstructorFingerprint.method.addInstructions(
            0,
            """
                # Full HD format option is available.
                invoke-static { p1 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->unlockOfflineSetting(Z)Z
                move-result p1

                # Download quality is enabled.
                invoke-static { p3 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->unlockOfflineSetting(Z)Z
                move-result p3
            """
        )

        // Offer 1080p on screens that are less than 1080 pixels wide.
        OfflineSettingsConstructorFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p3 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->getMaxDownloadQualityHeight(I)I
                move-result p3
            """
        )

        // Commands run by the extension have no interaction logger, which saving a video
        // can use without checking it is set. The logger interface has a logger that logs nothing.
        // Everything reading the logger from a context uses the same type, so any match will do.
        val interactionLoggerType = InteractionLoggerContextFingerprint.matchAll().first()
            .instructionMatches.last().instruction.getReference<TypeReference>()!!.type
        val noOpInteractionLogger = classDefBy(interactionLoggerType).fields.single { field ->
            AccessFlags.STATIC.isSet(field.accessFlags) && field.type == interactionLoggerType
        }
        NoOpInteractionLoggerFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $interactionLoggerType->${noOpInteractionLogger.name}:$interactionLoggerType
                return-object v0
            """
        )

        YouTubeOfflineVideoEndpointFingerprint.method.addInstructions(
            0,
            "invoke-static/range { p3 .. p4 }, Lapp/morphe/extension/youtube/sponsorblock/SponsorBlockOfflineStorage;->offlineVideoAction(Ljava/lang/String;Ljava/lang/Enum;)V"
        )

        // endregion
    }
}
