package app.revanced.patches.youtube.misc.test

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstructions
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.youtube.utils.fix.clientspoof.UserAgentClientSpoofPatch
import app.revanced.patches.youtube.utils.fix.clientspoof.SpoofClientResourcePatch
import app.revanced.patches.youtube.utils.fix.clientspoof.fingerprints.BuildInitPlaybackRequestFingerprint
import app.revanced.patches.youtube.utils.fix.clientspoof.fingerprints.BuildPlayerRequestURIFingerprint
import app.revanced.patches.youtube.utils.fix.clientspoof.fingerprints.CreatePlayerRequestBodyFingerprint
import app.revanced.patches.youtube.utils.fix.clientspoof.fingerprints.SetPlayerRequestClientTypeFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.*
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.playerresponse.PlayerResponseMethodHookPatch
import app.revanced.util.getReference
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Patch(
    dependencies = [
        SpoofClientResourcePatch::class,
        PlayerResponseMethodHookPatch::class,
        SettingsPatch::class,
        UserAgentClientSpoofPatch::class,
    ]
)
object SpoofTestClientPatch : BaseBytecodePatch(
    name = "Spoof client",
    description = "Spoofs the client to allow video playback.",
    dependencies = setOf(
        SpoofClientResourcePatch::class,
        PlayerResponseMethodHookPatch::class,
        SettingsPatch::class,
        UserAgentClientSpoofPatch::class,
    ),
    fingerprints = setOf(
        // Client type spoof.
        BuildInitPlaybackRequestFingerprint,
        BuildPlayerRequestURIFingerprint,
        SetPlayerRequestClientTypeFingerprint,
        CreatePlayerRequestBodyFingerprint,

        // Storyboard spoof.
        StoryboardRendererSpecFingerprint,
        PlayerResponseModelStoryboardRecommendedLevelFingerprint,
        StoryboardRendererDecoderRecommendedLevelFingerprint,
        PlayerResponseModelGeneralStoryboardRendererFingerprint,
        StoryboardRendererDecoderSpecFingerprint,
        StoryboardRendererSpecFingerprint,
    ),
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "Lapp/revanced/integrations/youtube/patches/spoof/SpoofClientPatch;"
    private const val CLIENT_INFO_CLASS_DESCRIPTOR =
        "Lcom/google/protos/youtube/api/innertube/InnertubeContext\$ClientInfo;"

    override fun execute(context: BytecodeContext) {

        // region Block /initplayback requests to fall back to /get_watch requests.
        BuildInitPlaybackRequestFingerprint.resultOrThrow().let {
            val moveUriStringIndex = it.scanResult.patternScanResult!!.startIndex

            it.mutableMethod.apply {
                val targetRegister = getInstruction<OneRegisterInstruction>(moveUriStringIndex).registerA

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

        // region Block /get_watch requests to fall back to /player requests.

        BuildPlayerRequestURIFingerprint.resultOrThrow().let {
            val invokeToStringIndex = it.scanResult.patternScanResult!!.startIndex

            it.mutableMethod.apply {
                val uriRegister = getInstruction<FiveRegisterInstruction>(invokeToStringIndex).registerC

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

        // region Get field references to be used below.

        val (clientInfoField, clientInfoClientTypeField, clientInfoClientVersionField) = SetPlayerRequestClientTypeFingerprint.resultOrThrow()
            .let { result ->
                // Field in the player request object that holds the client info object.
                val clientInfoField = result.mutableMethod.getInstructions().first { instruction ->
                        // requestMessage.clientInfo = clientInfoBuilder.build();
                        instruction.opcode == Opcode.IPUT_OBJECT && instruction.getReference<FieldReference>()?.type == CLIENT_INFO_CLASS_DESCRIPTOR
                    }.getReference<FieldReference>() ?: throw PatchException("Could not find clientInfoField")

                // Client info object's client type field.
                val clientInfoClientTypeField =
                    result.mutableMethod.getInstruction(result.scanResult.patternScanResult!!.endIndex)
                        .getReference<FieldReference>()
                        ?: throw PatchException("Could not find clientInfoClientTypeField")

                // Client info object's client version field.
                val clientInfoClientVersionField =
                    result.mutableMethod.getInstruction(result.scanResult.stringsScanResult!!.matches.first().index + 1)
                        .getReference<FieldReference>()
                        ?: throw PatchException("Could not find clientInfoClientVersionField")

                Triple(clientInfoField, clientInfoClientTypeField, clientInfoClientVersionField)
            }

        // endregion

        // region Spoof client type for /player requests.

        CreatePlayerRequestBodyFingerprint.resultOrThrow().let { result ->
            val setClientInfoMethodName = "patch_setClientInfo"
            val checkCastIndex = result.scanResult.patternScanResult!!.startIndex
            var clientInfoContainerClassName: String

            result.mutableMethod.apply {
                val checkCastInstruction = getInstruction<OneRegisterInstruction>(checkCastIndex)
                val requestMessageInstanceRegister = checkCastInstruction.registerA
                clientInfoContainerClassName = checkCastInstruction.getReference<TypeReference>()!!.type

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static { v$requestMessageInstanceRegister }," + " ${result.classDef.type}->$setClientInfoMethodName($clientInfoContainerClassName)V",
                )
            }

            // Change requestMessage.clientInfo.clientType and requestMessage.clientInfo.clientVersion to the spoofed values.
            // Do this in a helper method, to remove the need of picking out multiple free registers from the hooked code.
            result.mutableClass.methods.add(
                ImmutableMethod(
                    result.mutableClass.type,
                    setClientInfoMethodName,
                    listOf(ImmutableMethodParameter(clientInfoContainerClassName, null, "clientInfoContainer")),
                    "V",
                    AccessFlags.PRIVATE or AccessFlags.STATIC,
                    null,
                    null,
                    MutableMethodImplementation(3),
                ).toMutable().apply {
                    addInstructions(
                        """
                            invoke-static { }, $INTEGRATIONS_CLASS_DESCRIPTOR->isClientSpoofingEnabled()Z
                            move-result v0
                            if-eqz v0, :disabled
                            
                            iget-object v0, p0, $clientInfoField
                            
                            # Set client type to the spoofed value.
                            iget v1, v0, $clientInfoClientTypeField
                            invoke-static { v1 }, $INTEGRATIONS_CLASS_DESCRIPTOR->getClientTypeId(I)I
                            move-result v1
                            iput v1, v0, $clientInfoClientTypeField
                            
                            # Set client version to the spoofed value.
                            iget-object v1, v0, $clientInfoClientVersionField
                            invoke-static { v1 }, $INTEGRATIONS_CLASS_DESCRIPTOR->getClientVersion(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $clientInfoClientVersionField
                            
                            :disabled
                            return-void
                        """,
                    )
                },
            )
        }

        // endregion

        // region Fix storyboard if Android Testsuite is used.

        PlayerResponseMethodHookPatch += PlayerResponseMethodHookPatch.Hook.PlayerParameter(
            "$INTEGRATIONS_CLASS_DESCRIPTOR->setPlayerResponseVideoId(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;",
        )

        // Hook recommended seekbar thumbnails quality level for regular videos.
        StoryboardRendererDecoderRecommendedLevelFingerprint.resultOrThrow().let {
            val endIndex = it.scanResult.patternScanResult!!.endIndex

            it.mutableMethod.apply {
                val originalValueRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

                addInstructions(
                    endIndex + 1,
                    """
                        invoke-static { v$originalValueRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getRecommendedLevel(I)I
                        move-result v$originalValueRegister
                    """,
                )
            }
        }

        // Hook the recommended precise seeking thumbnails quality.
        PlayerResponseModelStoryboardRecommendedLevelFingerprint.resultOrThrow().let {
            val endIndex = it.scanResult.patternScanResult!!.endIndex

            it.mutableMethod.apply {
                val originalValueRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

                addInstructions(
                    endIndex,
                    """
                        invoke-static { v$originalValueRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getRecommendedLevel(I)I
                        move-result v$originalValueRegister
                    """,
                )
            }
        }

        // TODO: Hook the seekbar recommended level for Shorts to fix Shorts low quality seekbar thumbnails.

        /**
         * Hook StoryBoard renderer url.
         */
        StoryboardRendererDecoderRecommendedLevelFingerprint.resultOrThrow().let {
            val getStoryBoardIndex = it.scanResult.patternScanResult!!.endIndex

            it.mutableMethod.apply {
                val getStoryBoardRegister = getInstruction<OneRegisterInstruction>(getStoryBoardIndex).registerA

                addInstructions(
                    getStoryBoardIndex,
                    """
                        invoke-static { v$getStoryBoardRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getStoryboardRendererSpec(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$getStoryBoardRegister
                    """,
                )
            }
        }

        // Hook the seekbar thumbnail decoder, required for Shorts.
        StoryboardRendererDecoderSpecFingerprint.resultOrThrow().let {
            val storyBoardUrlIndex = it.scanResult.patternScanResult!!.startIndex + 1

            it.mutableMethod.apply {
                val getStoryBoardRegister = getInstruction<OneRegisterInstruction>(storyBoardUrlIndex).registerA

                addInstructions(
                    storyBoardUrlIndex + 1,
                    """
                        invoke-static { v$getStoryBoardRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getStoryboardRendererSpec(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$getStoryBoardRegister
                    """,
                )
            }
        }

        StoryboardRendererSpecFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val storyBoardUrlParams = "p0"

                addInstructions(
                    0,
                    """
                        if-nez $storyBoardUrlParams, :ignore
                        invoke-static { $storyBoardUrlParams }, $INTEGRATIONS_CLASS_DESCRIPTOR->getStoryboardRendererSpec(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object $storyBoardUrlParams
                        :ignore
                        nop
                    """,
                )
            }
        }

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MISC_EXPERIMENTAL_FLAGS", "SETTINGS: SPOOF_TEST_CLIENT"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
