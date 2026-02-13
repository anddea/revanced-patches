package app.morphe.patches.youtube.general.livering

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.CHANGE_LIVE_RING_CLICK_ACTION
import app.morphe.patches.youtube.utils.playservice.is_19_25_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.playbackstart.PLAYBACK_START_DESCRIPTOR_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.video.playbackstart.playbackStartDescriptorPatch
import app.morphe.patches.youtube.video.playbackstart.playbackStartVideoIdReference
import app.morphe.patches.youtube.video.playbackstart.shortsPlaybackStartIntentFingerprint
import app.morphe.patches.youtube.video.playbackstart.shortsPlaybackStartIntentLegacyFingerprint
import app.morphe.util.copyXmlNode
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private val openChannelOfLiveAvatarResourcePatch = resourcePatch(
    description = "openChannelOfLiveAvatarResourcePatch"
) {
    execute {
        arrayOf(
            "", "af", "am", "ar", "as", "az", "b+sr+Latn", "be", "bg", "bn", "bs", "ca",
            "cs", "da", "de", "el", "en-rGB", "en-rIN", "es", "es-rUS", "et", "eu", "fa",
            "fi", "fr", "fr-rCA", "gl", "gu", "hi", "hr", "hu", "hy", "in", "is", "it",
            "iw", "ja", "ka", "kk", "km", "kn", "ko", "ky", "lo", "lt", "lv", "mk", "ml",
            "mn", "mr", "ms", "my", "nb", "ne", "nl", "or", "pa", "pl", "pt", "pt-rBR",
            "pt-rPT", "ro", "ru", "si", "sk", "sl", "sq", "sr", "sv", "sw", "ta", "te",
            "th", "tl", "tr", "uk", "ur", "uz", "vi", "zh-rCN", "zh-rHK", "zh-rTW", "zu"
        ).forEach { locale ->
            val directory = if (locale.isEmpty())
                "values"
            else
                "values-$locale"

            copyXmlNode("youtube/livering/host", "$directory/strings.xml", "resources")
        }
    }
}

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/OpenChannelOfLiveAvatarPatch;"

@Suppress("unused")
val openChannelOfLiveAvatarPatch = bytecodePatch(
    CHANGE_LIVE_RING_CLICK_ACTION.title,
    CHANGE_LIVE_RING_CLICK_ACTION.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        openChannelOfLiveAvatarResourcePatch,
        playbackStartDescriptorPatch,
        versionCheckPatch,
    )

    execute {

        clientSettingEndpointFingerprint.methodOrThrow().apply {
            val eqzIndex = indexOfFirstInstructionReversedOrThrow(Opcode.IF_EQZ)
            var freeIndex = indexOfFirstInstructionReversedOrThrow(eqzIndex, Opcode.NEW_INSTANCE)
            var freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

            addInstructionsWithLabels(
                eqzIndex, """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->openChannel()Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :ignore
                    return-void
                    :ignore
                    nop
                    """
            )

            val playbackStartIndex = indexOfPlaybackStartDescriptorInstruction(this) + 1
            val playbackStartRegister =
                getInstruction<OneRegisterInstruction>(playbackStartIndex).registerA

            val mapIndex = indexOfFirstInstructionOrThrow(playbackStartIndex) {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_STATIC &&
                        reference?.returnType == "Ljava/lang/Object;" &&
                        reference.parameterTypes.firstOrNull() == "Ljava/util/Map;"
            }
            val mapRegister = getInstruction<FiveRegisterInstruction>(mapIndex).registerC

            freeIndex = indexOfFirstInstructionOrThrow(playbackStartIndex, Opcode.CONST_STRING)
            freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

            addInstructions(
                playbackStartIndex + 1, """
                    invoke-virtual { v$playbackStartRegister }, $playbackStartVideoIdReference
                    move-result-object v$freeRegister
                    invoke-static { v$mapRegister, v$freeRegister }, $EXTENSION_CLASS_DESCRIPTOR->fetchChannelId(Ljava/util/Map;Ljava/lang/String;)V
                    """
            )
        }

        fun MutableMethod.openChannel() =
            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    val reference = (instruction as? ReferenceInstruction)?.reference
                    instruction.opcode == Opcode.NEW_INSTANCE &&
                            reference is TypeReference &&
                            reference.type == "Landroid/os/Bundle;"
                }
                .map { (index, _) -> index }
                .reversed()
                .forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructionsWithLabels(
                        index, """
                            invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->openChannel()Z
                            move-result v$register
                            if-eqz v$register, :ignore
                            return-void
                            :ignore
                            nop
                            """
                    )
                }


        fun fetchChannelIdInstructions(
            playbackStartRegister: Int,
            mapRegister: Int,
            videoIdRegister: Int,
        ) =
            """
                invoke-virtual { v$playbackStartRegister }, $playbackStartVideoIdReference
                move-result-object v$videoIdRegister
                invoke-static { v$mapRegister, v$videoIdRegister }, $EXTENSION_CLASS_DESCRIPTOR->fetchChannelId(Ljava/util/Map;Ljava/lang/String;)V
            """

        if (is_19_25_or_greater) {
            shortsPlaybackStartIntentFingerprint.methodOrThrow().apply {
                openChannel()

                addInstructionsWithLabels(
                    0, """
                        move-object/from16 v0, p1
                        move-object/from16 v1, p2
                        ${fetchChannelIdInstructions(0, 1, 2)}
                        """
                )
            }
        } else {
            shortsPlaybackStartIntentLegacyFingerprint.methodOrThrow().apply {
                openChannel()

                val playbackStartIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.returnType == PLAYBACK_START_DESCRIPTOR_CLASS_DESCRIPTOR
                }
                val mapIndex =
                    indexOfFirstInstructionReversedOrThrow(playbackStartIndex, Opcode.IPUT)
                val mapRegister = getInstruction<TwoRegisterInstruction>(mapIndex).registerA
                val playbackStartRegister =
                    getInstruction<OneRegisterInstruction>(playbackStartIndex + 1).registerA
                val videoIdRegister =
                    getInstruction<FiveRegisterInstruction>(playbackStartIndex).registerC

                addInstructionsWithLabels(
                    playbackStartIndex + 2, """
                        move-object/from16 v$mapRegister, p2
                        ${
                        fetchChannelIdInstructions(
                            playbackStartRegister,
                            mapRegister,
                            videoIdRegister
                        )
                    }
                        """
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: CHANGE_LIVE_RING_CLICK_ACTION"
            ),
            CHANGE_LIVE_RING_CLICK_ACTION
        )

        // endregion

    }
}
