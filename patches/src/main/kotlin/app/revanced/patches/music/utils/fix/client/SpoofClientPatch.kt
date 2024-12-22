package app.revanced.patches.music.utils.fix.client

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.music.utils.compatibility.Constants
import app.revanced.patches.music.utils.extension.Constants.MISC_PATH
import app.revanced.patches.music.utils.patch.PatchList.SPOOF_CLIENT
import app.revanced.patches.music.utils.playbackSpeedBottomSheetFingerprint
import app.revanced.patches.music.utils.playservice.is_7_25_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.revanced.patches.shared.indexOfModelInstruction
import app.revanced.util.Utils.printWarn
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/SpoofClientPatch;"
private const val CLIENT_INFO_CLASS_DESCRIPTOR =
    "Lcom/google/protos/youtube/api/innertube/InnertubeContext\$ClientInfo;"

@Suppress("unused")
val spoofClientPatch = bytecodePatch(
    SPOOF_CLIENT.title,
    SPOOF_CLIENT.summary,
    false,
) {
    compatibleWith(
        Constants.YOUTUBE_MUSIC_PACKAGE_NAME(
            "6.20.51",
            "6.29.59",
            "6.42.55",
            "6.51.53",
            "7.16.53",
        ),
    )

    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        if (is_7_25_or_greater) {
            printWarn("\"${SPOOF_CLIENT.title}\" is not supported in this version. Use YouTube Music 7.24.51 or earlier.")
            return@execute
        }

        // region Get field references to be used below.

        val (clientInfoField, clientInfoClientTypeField, clientInfoClientVersionField) =
            setPlayerRequestClientTypeFingerprint.matchOrThrow().let { result ->
                with(result.method) {
                    // Field in the player request object that holds the client info object.
                    val clientInfoField = instructions.find { instruction ->
                        // requestMessage.clientInfo = clientInfoBuilder.build();
                        instruction.opcode == Opcode.IPUT_OBJECT &&
                                instruction.getReference<FieldReference>()?.type == CLIENT_INFO_CLASS_DESCRIPTOR
                    }?.getReference<FieldReference>()
                        ?: throw PatchException("Could not find clientInfoField")

                    // Client info object's client type field.
                    val clientInfoClientTypeField =
                        getInstruction(result.patternMatch!!.endIndex)
                            .getReference<FieldReference>()
                            ?: throw PatchException("Could not find clientInfoClientTypeField")

                    val clientInfoVersionIndex = result.stringMatches!!.first().index
                    val clientInfoVersionRegister =
                        getInstruction<OneRegisterInstruction>(clientInfoVersionIndex).registerA
                    val clientInfoClientVersionFieldIndex =
                        indexOfFirstInstructionOrThrow(clientInfoVersionIndex) {
                            opcode == Opcode.IPUT_OBJECT &&
                                    (this as TwoRegisterInstruction).registerA == clientInfoVersionRegister
                        }

                    // Client info object's client version field.
                    val clientInfoClientVersionField =
                        getInstruction(clientInfoClientVersionFieldIndex)
                            .getReference<FieldReference>()
                            ?: throw PatchException("Could not find clientInfoClientVersionField")

                    Triple(clientInfoField, clientInfoClientTypeField, clientInfoClientVersionField)
                }
            }

        val clientInfoClientModelField =
            with(createPlayerRequestBodyWithModelFingerprint.methodOrThrow()) {
                // The next IPUT_OBJECT instruction after getting the client model is setting the client model field.
                val clientInfoClientModelIndex =
                    indexOfFirstInstructionOrThrow(indexOfModelInstruction(this)) {
                        val reference = getReference<FieldReference>()
                        opcode == Opcode.IPUT_OBJECT &&
                                reference?.definingClass == CLIENT_INFO_CLASS_DESCRIPTOR &&
                                reference.type == "Ljava/lang/String;"
                    }
                getInstruction<ReferenceInstruction>(clientInfoClientModelIndex).reference
            }

        val clientInfoOsVersionField =
            with(createPlayerRequestBodyWithVersionReleaseFingerprint.methodOrThrow()) {
                val buildIndex = indexOfBuildInstruction(this)
                val clientInfoOsVersionIndex = indexOfFirstInstructionOrThrow(buildIndex - 5) {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IPUT_OBJECT &&
                            reference?.definingClass == CLIENT_INFO_CLASS_DESCRIPTOR &&
                            reference.type == "Ljava/lang/String;"
                }
                getInstruction<ReferenceInstruction>(clientInfoOsVersionIndex).reference
            }

        // endregion

        // region Spoof client type for /player requests.

        createPlayerRequestBodyFingerprint.matchOrThrow().let {
            it.method.apply {
                val setClientInfoMethodName = "setClientInfo"
                val checkCastIndex = it.patternMatch!!.startIndex

                val checkCastInstruction = getInstruction<OneRegisterInstruction>(checkCastIndex)
                val requestMessageInstanceRegister = checkCastInstruction.registerA
                val clientInfoContainerClassName =
                    checkCastInstruction.getReference<TypeReference>()!!.type

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static { v$requestMessageInstanceRegister }," +
                            " $definingClass->$setClientInfoMethodName($clientInfoContainerClassName)V",
                )

                // Change client info to use the spoofed values.
                // Do this in a helper method, to remove the need of picking out multiple free registers from the hooked code.
                it.classDef.methods.add(
                    ImmutableMethod(
                        definingClass,
                        setClientInfoMethodName,
                        listOf(
                            ImmutableMethodParameter(
                                clientInfoContainerClassName,
                                annotations,
                                "clientInfoContainer"
                            )
                        ),
                        "V",
                        AccessFlags.PRIVATE or AccessFlags.STATIC,
                        annotations,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructions(
                            """
                                invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->isClientSpoofingEnabled()Z
                                move-result v0
                                if-eqz v0, :disabled
                            
                                iget-object v0, p0, $clientInfoField
                            
                                # Set client type to the spoofed value.
                                iget v1, v0, $clientInfoClientTypeField
                                invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientTypeId(I)I
                                move-result v1
                                iput v1, v0, $clientInfoClientTypeField

                                # Set client model to the spoofed value.
                                iget-object v1, v0, $clientInfoClientModelField
                                invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientModel(Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v1
                                iput-object v1, v0, $clientInfoClientModelField
                            
                                # Set client version to the spoofed value.
                                iget-object v1, v0, $clientInfoClientVersionField
                                invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientVersion(Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v1
                                iput-object v1, v0, $clientInfoClientVersionField

                                # Set client os version to the spoofed value.
                                iget-object v1, v0, $clientInfoOsVersionField
                                invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getOsVersion(Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v1
                                iput-object v1, v0, $clientInfoOsVersionField

                                :disabled
                                return-void
                                """,
                        )
                    },
                )
            }
        }

        // endregion

        // region Spoof user-agent

        userAgentHeaderBuilderFingerprint.methodOrThrow().apply {
            val insertIndex = implementation!!.instructions.lastIndex
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static { v$insertRegister }, $EXTENSION_CLASS_DESCRIPTOR->getUserAgent(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$insertRegister
                    """
            )
        }

        // endregion

        // region fix for playback speed menu is not available in Podcasts

        playbackSpeedBottomSheetFingerprint.mutableClassOrThrow().let {
            val onItemClickMethod =
                it.methods.find { method -> method.name == "onItemClick" }
                    ?: throw PatchException("Failed to find onItemClick method")

            onItemClickMethod.apply {
                val createPlaybackSpeedMenuItemIndex = indexOfFirstInstructionReversedOrThrow {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.returnType == "V" &&
                            reference.parameterTypes.firstOrNull()?.startsWith("[L") == true
                }
                val createPlaybackSpeedMenuItemMethod =
                    getWalkerMethod(createPlaybackSpeedMenuItemIndex)
                createPlaybackSpeedMenuItemMethod.apply {
                    val shouldCreateMenuIndex = indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                reference?.returnType == "Z" &&
                                reference.parameterTypes.isEmpty()
                    } + 2
                    val shouldCreateMenuRegister =
                        getInstruction<OneRegisterInstruction>(shouldCreateMenuIndex - 1).registerA

                    addInstructions(
                        shouldCreateMenuIndex,
                        """
                            invoke-static { v$shouldCreateMenuRegister }, $EXTENSION_CLASS_DESCRIPTOR->forceCreatePlaybackSpeedMenu(Z)Z
                            move-result v$shouldCreateMenuRegister
                            """,
                    )
                }
            }
        }

        // endregion

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_spoof_client",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_spoof_client_type",
            "revanced_spoof_client",
        )

        updatePatchStatus(SPOOF_CLIENT)

    }
}
