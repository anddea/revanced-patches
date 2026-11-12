/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.player.action

import app.morphe.patcher.Fingerprint
import app.morphe.patches.youtube.general.spoofappversion.Endpoint
import app.morphe.patches.youtube.general.spoofappversion.addClientVersionHook
import app.morphe.patches.youtube.general.spoofappversion.clientContextHookPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patches.shared.misc.fix.proto.immutableMethodRef
import app.morphe.patches.shared.misc.fix.proto.mutableCopyMethodRef
import app.morphe.patches.youtube.utils.commandrouter.parseProtoInstructions
import app.morphe.patches.youtube.utils.proto.elementProtoParserHookPatch
import app.morphe.patches.youtube.utils.proto.hookElement
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.registersUsed
import app.morphe.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.shared.misc.request.buildRequestPatch
import app.morphe.patches.shared.misc.request.hookBuildRequest
import app.morphe.patches.shared.misc.spoof.BuildInnerTubeProtoRequestUriFingerprint
import app.morphe.patches.shared.misc.spoof.addMediaSessionOverride
import app.morphe.patches.youtube.shared.WatchNextResponseParserFingerprint
import app.morphe.patches.youtube.utils.auth.authHookPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.componentlist.hookElementList
import app.morphe.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.fix.hype.hypeButtonIconPatch
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_ACTION_BUTTONS
import app.morphe.patches.youtube.utils.playservice.is_20_30_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_39_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.morphe.patches.youtube.video.videoid.videoIdPatch
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/RestoreOldVideoActionBarPatch;"

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ActionButtonsFilter;"
private const val ACTION_BUTTONS_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/ActionButtonsPatch;"
private const val EXTENSION_CONFIG_INFO_INTERFACE =
    $$"$$GENERAL_PATH/GeneralPatch$ConfigInfoInterface;"
private const val EXTENSION_REQUEST_INTERFACE =
    $$"$$GENERAL_PATH/GeneralPatch$RequestInterface;"

private object ModernRelateVideoOverlayFingerprint : Fingerprint(
    filters = listOf(
        literal(45614162L)
    )
)

private object RelateVideoOverlayLayoutParamFingerprint : Fingerprint(
    filters = listOf(
        literal(45661108L)
    )
)

private object BuildInnerTubeProtoRequestBodyFingerprint : Fingerprint(
    classFingerprint = BuildInnerTubeProtoRequestUriFingerprint,
    parameters = listOf("L"),
    returnType = "Lcom/google/protobuf/MessageLite;",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = CLIENT_INFO_CLASS_DESCRIPTOR
        )
    )
)

/** Matches the request bodies of every request class, including the watch-page request. */
private object InnerTubeRequestBodyFingerprint : Fingerprint(
    parameters = listOf("L"),
    returnType = "Lcom/google/protobuf/MessageLite;",
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = CLIENT_INFO_CLASS_DESCRIPTOR)
    )
)

private fun getConfigInfoFingerprint(configInfoClass: String) = object : Fingerprint(
    definingClass = configInfoClass,
    name = "<init>",
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        )
    )
) {}

internal val restoreOldVideoActionBarPatch = bytecodePatch(
    description = "restoreOldVideoActionBarPatch"
) {
    dependsOn(
        settingsPatch,
        clientContextHookPatch,
        buildRequestPatch,
        elementProtoParserHookPatch,
        fixProtoLibraryPatch,
        versionCheckPatch,
    )

    execute {
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "SETTINGS: VIDEO_ACTION_BAR_LAYOUT"
            )
        )

        // The layout of the action bar is a field of its element, so the Classic and Minimal layouts
        // do not depend on the layout that the server sends.
        hookElement("$EXTENSION_CLASS->setVideoActionBarLayout([B)[B")

        // The channel name below the title of the watch page opens the channel
        // instead of the description, as the rest of the text below the title does.
        hookElement("$EXTENSION_CLASS->removeSubtitleChannelCommand([B)[B")

        // The two rows of the Classic Two-Rows layout are two elements, and the server sends one.
        watchNextResultsFingerprint(
            immutableMethodRef.get()!!.definingClass,
            with (WatchNextResponseParserFingerprint) {
                clearMatch() // Fingerprint is shared and indexes may no longer be correct.
                instructionMatches[2].fieldReference.definingClass
            }
        ).let {
            val sectionsField = it.instructionMatches.first().fieldReference
            val sectionClass = it.instructionMatches.last().fieldReference.definingClass
            val makeImmutableMethod = classDefBy(sectionsField.type).methods.single { method ->
                method.returnType == "V" && method.parameterTypes.isEmpty()
            }

            it.method.apply {
                val helperMethod = ImmutableMethod(
                    definingClass,
                    "patch_setTwoRowsSection",
                    listOf(ImmutableMethodParameter(parameterTypes.first().toString(), null, null)),
                    "V",
                    AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                    null,
                    null,
                    MutableMethodImplementation(8),
                ).toMutable().apply {
                    addInstructionsWithLabels(
                        0,
                        """
                            iget-object v0, p1, $sectionsField
                            invoke-interface { v0 }, Ljava/util/List;->isEmpty()Z
                            move-result v1
                            if-nez v1, :exit

                            # The section of the channel and the buttons is the first section.
                            # The others may have been changed in place by 'Hide related videos',
                            # and cannot be serialized.
                            const/4 v2, 0x0
                            invoke-interface { v0, v2 }, Ljava/util/List;->get(I)Ljava/lang/Object;
                            move-result-object v3
                            check-cast v3, Lcom/google/protobuf/MessageLite;
                            invoke-static { v3 }, $EXTENSION_CLASS->getTwoRowsSection(Lcom/google/protobuf/MessageLite;)[B
                            move-result-object v3
                            if-eqz v3, :exit

                            ${parseProtoInstructions(sectionClass, "v3", "v4", "v5")}
                            check-cast v4, $sectionClass

                            # The list of a parsed response cannot be modified. 'Hide related videos'
                            # relies on it, so the new list cannot be modified either.
                            invoke-interface { v0 }, ${immutableMethodRef.get()!!}
                            move-result v5
                            if-nez v5, :set
                            invoke-static { v0 }, ${mutableCopyMethodRef.get()!!}
                            move-result-object v0

                            :set
                            invoke-interface { v0, v2, v4 }, Ljava/util/List;->set(ILjava/lang/Object;)Ljava/lang/Object;
                            if-nez v5, :exit
                            invoke-interface { v0 }, $makeImmutableMethod
                            iput-object v0, p1, $sectionsField

                            :exit
                            return-void
                        """
                    )
                }

                it.classDef.methods.add(helperMethod)

                addInstruction(
                    0,
                    "invoke-direct { p0, p1 }, $helperMethod"
                )
            }
        }

        if (is_20_39_or_greater) {
            addMediaSessionOverride("$GENERAL_CLASS_DESCRIPTOR->useMediaSessionFeatureFlag(Z)Z")
        }

        if (is_20_30_or_greater) {
            hookBuildRequest(
                descriptor = "$GENERAL_CLASS_DESCRIPTOR->fixVideoActionBar(Ljava/lang/String;Ljava/util/Map;)Ljava/util/Map;",
                hookHeader = true,
            )

            // Comment requests use the same 'next' endpoint as watch page requests.
            // Their config must not be overridden, so find out which requests are for the watch page.
            // Some app versions build requests in more than one class (21.16 has two, and the watch
            // page 'next' request uses the second one), so every request class is hooked.
            BuildInnerTubeProtoRequestUriFingerprint.matchAll().forEach { uriMatch ->
                // uri.buildUpon().appendEncodedPath("youtubei/v1").appendEncodedPath(this.request.endpoint)
                val (requestField, endpointField) = uriMatch.method.let { method ->
                    val appendEndpointIndex = method.indexOfFirstInstructionReversedOrThrow {
                        getReference<MethodReference>()?.name == "appendEncodedPath"
                    }
                    val endpointRegister = method.getInstruction<FiveRegisterInstruction>(appendEndpointIndex).registerD
                    val endpointIndex = method.indexOfFirstInstructionReversedOrThrow(appendEndpointIndex) {
                        opcode == Opcode.IGET_OBJECT && (this as TwoRegisterInstruction).registerA == endpointRegister
                    }
                    val requestRegister = method.getInstruction<TwoRegisterInstruction>(endpointIndex).registerB
                    val requestIndex = method.indexOfFirstInstructionReversedOrThrow(endpointIndex) {
                        opcode == Opcode.IGET_OBJECT && (this as TwoRegisterInstruction).registerA == requestRegister
                    }
                    Pair(
                        method.getInstruction<ReferenceInstruction>(requestIndex).reference as FieldReference,
                        method.getInstruction<ReferenceInstruction>(endpointIndex).reference as FieldReference
                    )
                }

                uriMatch.classDef.apply {
                    interfaces.add(EXTENSION_REQUEST_INTERFACE)

                    methods.add(
                        ImmutableMethod(
                            type,
                            "patch_getEndpoint",
                            listOf(),
                            "Ljava/lang/String;",
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(2),
                        ).toMutable().apply {
                            addInstructions(
                                0,
                                """
                                    iget-object v0, p0, $requestField
                                    iget-object v0, v0, $endpointField
                                    return-object v0
                                """
                            )
                        }
                    )
                }

                InnerTubeRequestBodyFingerprint.matchAll(uriMatch.classDef).forEach { bodyMatch ->
                    bodyMatch.method.apply {
                        findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                            val register = getInstruction<OneRegisterInstruction>(index).registerA

                            addInstruction(
                                index,
                                "invoke-static { v$register, p0 }, $GENERAL_CLASS_DESCRIPTOR->" +
                                        "onBuildRequestBody(Lcom/google/protobuf/MessageLite;$EXTENSION_REQUEST_INTERFACE)V"
                            )
                        }
                    }
                }
            }

            val configInfoClass = with(BuildInnerTubeProtoRequestBodyFingerprint) {
                val match = instructionMatches.first()
                val index = match.index
                val instruction = match.instruction
                val register = instruction.registersUsed[0]

                method.addInstructions(
                    index,
                    "invoke-static { v$register }, $GENERAL_CLASS_DESCRIPTOR->fixVideoActionBar($EXTENSION_CONFIG_INFO_INTERFACE)V"
                )

                instruction.getReference<FieldReference>()!!.type
            }

            getConfigInfoFingerprint(configInfoClass).let {
                it.classDef.apply {
                    interfaces.add(EXTENSION_CONFIG_INFO_INTERFACE)

                    mapOf(
                        0 to "patch_setColdConfigData",
                        1 to "patch_setColdHashData",
                    ).forEach { (matchIndex, interfaceMethodName) ->
                        val coldDataField = it.instructionMatches[matchIndex]
                            .fieldReference

                        methods.add(
                            ImmutableMethod(
                                type,
                                interfaceMethodName,
                                listOf(ImmutableMethodParameter("Ljava/lang/String;", null, null)),
                                "V",
                                AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                                null,
                                null,
                                MutableMethodImplementation(3),
                            ).toMutable().apply {
                                addInstructions(
                                    0,
                                    """
                                        iput-object p1, p0, $coldDataField
                                        return-void
                                    """
                                )
                            }
                        )
                    }
                }
            }

            listOf(
                ModernRelateVideoOverlayFingerprint,
                RelateVideoOverlayLayoutParamFingerprint
            ).forEach { fingerprint ->
                fingerprint.clearMatch()
                fingerprint.matchAll().forEach {
                    it.method.insertLiteralOverride(
                        it.instructionMatches.first().index,
                        "$GENERAL_CLASS_DESCRIPTOR->fixRelatedVideoOverlay(Z)Z"
                    )
                }
            }
        } else {
            // In YT 20.29 or earlier, there are no issues even if the app version is spoofed.
            // Simply spoofing the app version (in the player).
            setOf(
                Endpoint.GET_WATCH,
                Endpoint.NEXT,
            ).forEach { endpoint ->
                addClientVersionHook(
                    endpoint,
                    "$EXTENSION_CLASS->getVideoActionBarAppVersionOverride(Ljava/lang/String;)Ljava/lang/String;",
                )
            }
        }
    }
}

@Suppress("unused")
val actionButtonsPatch = bytecodePatch(
    HIDE_ACTION_BUTTONS.title,
    HIDE_ACTION_BUTTONS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        lazilyConvertedElementHookPatch,
        fixProtoLibraryPatch,
        restoreOldVideoActionBarPatch,
        videoInformationPatch,
        videoIdPatch,
        authHookPatch,
        hypeButtonIconPatch,
    )

    execute {
        addLithoFilter(FILTER_CLASS_DESCRIPTOR)
        hookElementList("$FILTER_CLASS_DESCRIPTOR->onLazilyConvertedElementLoaded")

        // region patch for hide action buttons by index

        hookPlayerResponseVideoId("$ACTION_BUTTONS_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Z)V")
        hookElementList("$ACTION_BUTTONS_CLASS_DESCRIPTOR->hideActionButtonByIndex")

        // endregion

        WatchNextResponseParserFingerprint.let {
            it.clearMatch()
            it.method.apply {
                val index = it.instructionMatches[5].index
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $FILTER_CLASS_DESCRIPTOR->" +
                        "onSingleColumnWatchNextResultsLoaded(Lcom/google/protobuf/MessageLite;)V"
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: HIDE_ACTION_BUTTONS"
            ),
            HIDE_ACTION_BUTTONS
        )

        // endregion

    }
}
