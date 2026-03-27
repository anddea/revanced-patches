package app.morphe.patches.youtube.general.downloads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.HOOK_DOWNLOAD_ACTIONS
import app.morphe.patches.youtube.utils.pip.pipStateHookPatch
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playlist.playlistPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/DownloadActionsPatch;"

private const val OFFLINE_PLAYLIST_ENDPOINT_OUTER_CLASS_DESCRIPTOR =
    "Lcom/google/protos/youtube/api/innertube/OfflinePlaylistEndpointOuterClass${'$'}OfflinePlaylistEndpoint;"

@Suppress("unused")
val downloadActionsPatch = bytecodePatch(
    HOOK_DOWNLOAD_ACTIONS.title,
    HOOK_DOWNLOAD_ACTIONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        pipStateHookPatch,
        playlistPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        settingsPatch,
    )

    execute {

        // region patch for hook download actions (video action bar and flyout panel)

        offlineVideoEndpointFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static/range {p1 .. p3}, $EXTENSION_CLASS_DESCRIPTOR->inAppVideoDownloadButtonOnClick(Ljava/util/Map;Ljava/lang/Object;Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :show_native_downloader
                    return-void
                    """, ExternalLabel("show_native_downloader", getInstruction(0))
            )
        }

        // endregion

        // region patch for hook download actions (playlist)

        val onClickListenerClass =
            downloadPlaylistButtonOnClickFingerprint.methodOrThrow().let {
                val playlistDownloadActionInvokeIndex =
                    indexOfPlaylistDownloadActionInvokeInstruction(it)

                it.instructions.subList(
                    playlistDownloadActionInvokeIndex - 10,
                    playlistDownloadActionInvokeIndex,
                ).find { instruction ->
                    instruction.opcode == Opcode.INVOKE_VIRTUAL_RANGE
                            && instruction.getReference<MethodReference>()?.parameterTypes?.first() == "Ljava/lang/String;"
                }?.getReference<MethodReference>()?.returnType
                    ?: throw PatchException("Could not find onClickListenerClass")
            }

        findMethodOrThrow(onClickListenerClass) {
            name == "onClick"
        }.apply {
            val insertIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.name == "isEmpty"
            }
            val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerC

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->inAppPlaylistDownloadButtonOnClick(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$insertRegister
                    """
            )
        }

        offlinePlaylistEndpointFingerprint.methodOrThrow().apply {
            val playlistIdParameter = parameterTypes.indexOf("Ljava/lang/String;") + 1
            if (playlistIdParameter > 0) {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {p$playlistIdParameter}, $EXTENSION_CLASS_DESCRIPTOR->inAppPlaylistDownloadMenuOnClick(Ljava/lang/String;)Z
                        move-result v0
                        if-eqz v0, :show_native_downloader
                        return-void
                        """, ExternalLabel("show_native_downloader", getInstruction(0))
                )
            } else {
                val freeRegister = implementation!!.registerCount - parameters.size - 2

                val playlistIdIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IGET_OBJECT &&
                            reference?.definingClass == OFFLINE_PLAYLIST_ENDPOINT_OUTER_CLASS_DESCRIPTOR &&
                            reference.type == "Ljava/lang/String;"
                }
                val playlistIdReference =
                    getInstruction<ReferenceInstruction>(playlistIdIndex).reference

                val targetIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.CHECK_CAST &&
                            (this as? ReferenceInstruction)?.reference?.toString() == OFFLINE_PLAYLIST_ENDPOINT_OUTER_CLASS_DESCRIPTOR
                }
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructionsWithLabels(
                    targetIndex + 1,
                    """
                        iget-object v$freeRegister, v$targetRegister, $playlistIdReference
                        invoke-static {v$freeRegister}, $EXTENSION_CLASS_DESCRIPTOR->inAppPlaylistDownloadMenuOnClick(Ljava/lang/String;)Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :show_native_downloader
                        return-void
                        """,
                    ExternalLabel("show_native_downloader", getInstruction(targetIndex + 1))
                )
            }
        }

        // endregion

        // region patch for show the playlist download button

        setPlaylistDownloadButtonVisibilityFingerprint
            .matchOrThrow(accessibilityOfflineButtonSyncFingerprint).let {
                it.method.apply {
                    val insertIndex = it.instructionMatches.first().index + 2
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->overridePlaylistDownloadButtonVisibility(Z)Z
                            move-result v$insertRegister
                            """
                    )
                }
            }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: HOOK_BUTTONS",
                "SETTINGS: HOOK_DOWNLOAD_ACTIONS"
            ),
            HOOK_DOWNLOAD_ACTIONS
        )

        // endregion

    }
}
