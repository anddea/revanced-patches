package app.revanced.patches.music.utils.fix.client

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.music.utils.extension.Constants.SPOOF_PATH
import app.revanced.patches.music.utils.playbackRateBottomSheetClassFingerprint
import app.revanced.patches.music.utils.playbackSpeedBottomSheetFingerprint
import app.revanced.patches.music.utils.playservice.is_6_36_or_greater
import app.revanced.patches.music.utils.resourceid.varispeedUnavailableTitle
import app.revanced.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import app.revanced.patches.shared.clientTypeFingerprint
import app.revanced.patches.shared.createPlayerRequestBodyFingerprint
import app.revanced.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.revanced.patches.shared.indexOfBrandInstruction
import app.revanced.patches.shared.indexOfClientInfoInstruction
import app.revanced.patches.shared.indexOfManufacturerInstruction
import app.revanced.patches.shared.indexOfModelInstruction
import app.revanced.patches.shared.indexOfReleaseInstruction
import app.revanced.util.findFieldFromToString
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.fingerprint.originalMethodOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/SpoofClientPatch;"

context(BytecodePatchContext)
internal fun patchSpoofClient() {

    lateinit var clientInfoReference: Reference
    lateinit var clientIdReference: Reference
    lateinit var clientVersionReference: Reference
    lateinit var deviceBrandReference: Reference
    lateinit var deviceMakeReference: Reference
    lateinit var deviceModelReference: Reference
    lateinit var osNameReference: Reference
    lateinit var osVersionReference: Reference

    fun MutableMethod.getFieldReference(index: Int) =
        getInstruction<ReferenceInstruction>(index).reference

    // region Get field references to be used below.

    clientTypeFingerprint.matchOrThrow().let {
        it.method.apply {
            val clientInfoIndex = indexOfClientInfoInstruction(this)
            val clientIdIndex = it.patternMatch!!.endIndex
            val dummyClientVersionIndex = it.stringMatches!!.first().index
            val dummyClientVersionRegister =
                getInstruction<OneRegisterInstruction>(dummyClientVersionIndex).registerA
            val clientVersionIndex =
                indexOfFirstInstructionOrThrow(dummyClientVersionIndex) {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.type == "Ljava/lang/String;" &&
                            (this as TwoRegisterInstruction).registerA == dummyClientVersionRegister
                }

            clientInfoReference =
                getFieldReference(clientInfoIndex)
            clientIdReference =
                getFieldReference(clientIdIndex)
            clientVersionReference =
                getFieldReference(clientVersionIndex)
        }
    }

    fun MutableMethod.getClientInfoIndex(
        startIndex: Int,
        reversed: Boolean = false
    ): Int {
        val filter: Instruction.() -> Boolean = {
            val reference = getReference<FieldReference>()
            opcode == Opcode.IPUT_OBJECT &&
                    reference?.definingClass == CLIENT_INFO_CLASS_DESCRIPTOR &&
                    reference.type == "Ljava/lang/String;"
        }
        return if (reversed) {
            indexOfFirstInstructionReversedOrThrow(startIndex, filter)
        } else {
            indexOfFirstInstructionOrThrow(startIndex, filter)
        }
    }

    createPlayerRequestBodyWithModelFingerprint.methodOrThrow().apply {
        val buildManufacturerIndex =
            indexOfManufacturerInstruction(this)
        val deviceBrandIndex =
            getClientInfoIndex(indexOfBrandInstruction(this))
        val deviceMakeIndex =
            getClientInfoIndex(buildManufacturerIndex)
        val deviceModelIndex =
            getClientInfoIndex(indexOfModelInstruction(this))
        val chipSetIndex =
            getClientInfoIndex(buildManufacturerIndex, true)
        val osNameIndex =
            getClientInfoIndex(chipSetIndex - 1, true)
        val osVersionIndex =
            getClientInfoIndex(indexOfReleaseInstruction(this))

        deviceBrandReference =
            getFieldReference(deviceBrandIndex)
        deviceMakeReference =
            getFieldReference(deviceMakeIndex)
        deviceModelReference =
            getFieldReference(deviceModelIndex)
        osNameReference =
            getFieldReference(osNameIndex)
        osVersionReference =
            getFieldReference(osVersionIndex)
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
                "invoke-static { v$requestMessageInstanceRegister }, " +
                        "$definingClass->$setClientInfoMethodName($clientInfoContainerClassName)V",
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
                        0,
                        """
                            invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->isClientSpoofingEnabled()Z
                            move-result v0
                            if-eqz v0, :disabled
                            
                            iget-object v0, p0, $clientInfoReference
                            
                            # Set client id.
                            iget v1, v0, $clientIdReference
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientId(I)I
                            move-result v1
                            iput v1, v0, $clientIdReference
                            
                            # Set client version.
                            iget-object v1, v0, $clientVersionReference
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientVersion(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $clientVersionReference

                            # Set device brand.
                            iget-object v1, v0, $deviceBrandReference
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getDeviceBrand(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $deviceBrandReference

                            # Set device make.
                            iget-object v1, v0, $deviceMakeReference
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getDeviceMake(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $deviceMakeReference

                            # Set device model.
                            iget-object v1, v0, $deviceModelReference
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getDeviceModel(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $deviceModelReference

                            # Set os name.
                            iget-object v1, v0, $osNameReference
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getOSName(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $osNameReference

                            # Set os version.
                            iget-object v1, v0, $osVersionReference
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getOSVersion(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $osVersionReference

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

    // region fix for video action bar is always hidden

    if (is_6_36_or_greater) {
        spoofAppVersionFingerprint.matchOrThrow().let {
            it.method.apply {
                val startIndex = it.patternMatch!!.startIndex
                val buildOverrideNameRegister =
                    getInstruction<OneRegisterInstruction>(startIndex).registerA

                addInstructions(
                    startIndex + 1, """
                    invoke-static {v$buildOverrideNameRegister}, $EXTENSION_CLASS_DESCRIPTOR->getClientVersionOverride(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$buildOverrideNameRegister
                    """
                )
            }
        }
    }

    // endregion

    // region fix for playback speed menu is not available in Podcasts

    // for iOS Music
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

    // for Android Music
    playbackRateBottomSheetClassFingerprint.methodOrThrow().apply {
        val literalIndex =
            indexOfFirstLiteralInstructionOrThrow(varispeedUnavailableTitle)
        val insertIndex =
            indexOfFirstInstructionReversedOrThrow(literalIndex, Opcode.IF_EQZ)
        val insertRegister =
            getInstruction<OneRegisterInstruction>(insertIndex).registerA

        addInstructions(
            insertIndex,
            """
                invoke-static { v$insertRegister }, $EXTENSION_CLASS_DESCRIPTOR->forceCreatePlaybackSpeedMenuInverse(Z)Z
                move-result v$insertRegister
                """,
        )
    }

    // region fix for feature flags

    if (playbackFeatureFlagFingerprint.resolvable()) {
        playbackFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
            PLAYBACK_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->forceDisablePlaybackFeatureFlag(Z)Z"
        )
    }

    // endregion

    // region fix background playback in live stream, if spoofing to iOS

    val playerResponseModel = directorSavedStateToStringFingerprint
        .originalMethodOrThrow()
        .findFieldFromToString(INIT_PLAYER_RESPONSE)
        .type

    val backgroundPlaybackPlayerResponseFingerprint = legacyFingerprint(
        name = "backgroundPlaybackPlayerResponseFingerprint",
        returnType = "Z",
        accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
        parameters = listOf(playerResponseModel),
        opcodes = listOf(
            Opcode.CONST_4,
            Opcode.IF_EQZ,
            Opcode.INVOKE_INTERFACE,
            Opcode.MOVE_RESULT_OBJECT,
            Opcode.IF_EQZ,
            Opcode.INVOKE_INTERFACE,
            Opcode.MOVE_RESULT_OBJECT,
            Opcode.INVOKE_STATIC,
            Opcode.MOVE_RESULT,
            Opcode.IF_EQZ,
            Opcode.INVOKE_INTERFACE,
            Opcode.MOVE_RESULT,
            Opcode.CONST_4,
            Opcode.IF_EQZ,
            Opcode.INVOKE_INTERFACE,
            Opcode.MOVE_RESULT_OBJECT,
            Opcode.INVOKE_VIRTUAL,
            Opcode.MOVE_RESULT,
            Opcode.IF_NEZ,
        ),
    )

    backgroundPlaybackPlayerResponseFingerprint
        .methodOrThrow()
        .addInstructionsWithLabels(
            0, """
                invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->isClientSpoofingEnabled()Z
                move-result v0
                if-eqz v0, :disabled
                const/4 v0, 0x1
                return v0
                :disabled
                nop
                """
        )

    // endregion

}
