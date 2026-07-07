/*
 * Copyright (C) 2024-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.downloads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.HOOK_DOWNLOAD_ACTIONS
import app.morphe.patches.youtube.utils.pip.pipStateHookPatch
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.playlist.playlistPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findMethodOrThrow
import app.morphe.util.findFreeRegister
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.fingerprint.matchOrNull
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/DownloadActionsPatch;"

private const val EXTENSION_FLYOUT_MENU_VIDEO_ID_INTERFACE =
    "Lapp/morphe/extension/youtube/patches/general/DownloadActionsPatch${'$'}FlyoutMenuVideoIdInterface;"

private const val EXTENSION_PROTOCOL_BUFFER_INTERFACE =
    "Lapp/morphe/extension/youtube/patches/general/DownloadActionsPatch${'$'}ProtocolBufferFieldInterface;"

private const val OFFLINE_PLAYLIST_ENDPOINT_OUTER_CLASS_DESCRIPTOR =
    "Lcom/google/protos/youtube/api/innertube/OfflinePlaylistEndpointOuterClass${'$'}OfflinePlaylistEndpoint;"

@Suppress("unused")
val downloadActionsPatch = bytecodePatch(
    HOOK_DOWNLOAD_ACTIONS.title,
    HOOK_DOWNLOAD_ACTIONS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        sharedResourceIdPatch,
    )

    execute {

        // region patch Play next in queue flyout action

        FlyoutBufferClassFingerprint.let {
            val bufferField = it.classDef.fields.first { field -> field.type == "[B" }

            mutableClassDefBy(it.classDef.type).apply {
                interfaces.add(EXTENSION_PROTOCOL_BUFFER_INTERFACE)
                methods.add(
                    ImmutableMethod(
                        type,
                        "patch_getBuffer",
                        emptyList(),
                        "[B",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(2),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                iget-object v0, p0, $bufferField
                                return-object v0
                            """,
                        )
                    },
                )
            }
        }

        fun addFlyoutVideoIdInterface(messageType: String) {
            // The video ID is the only String field initialized to an empty value.
            val videoIdStringField = Fingerprint(
                definingClass = messageType,
                name = "<init>",
                filters = listOf(
                    string(""),
                    fieldAccess(
                        opcode = Opcode.IPUT_OBJECT,
                        definingClass = "this",
                        type = "Ljava/lang/String;",
                        location = MatchAfterWithin(2),
                    ),
                ),
            ).instructionMatches.last().instruction.getReference<FieldReference>()!!

            mutableClassDefBy(messageType).apply {
                interfaces.add(EXTENSION_FLYOUT_MENU_VIDEO_ID_INTERFACE)
                methods.add(
                    ImmutableMethod(
                        type,
                        "patch_getVideoId",
                        emptyList(),
                        "Ljava/lang/String;",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(2),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                iget-object v0, p0, $videoIdStringField
                                return-object v0
                            """,
                        )
                    },
                )
            }
        }

        // Full watch history does not use Litho.
        FlyoutMenuItemMessageFingerprint.let { fingerprint ->
            val match = fingerprint.matchOrNull()
            if (match != null) {
                val messageType = fingerprint.instructionMatches[1]
                    .instruction.getReference<TypeReference>()!!.type
                addFlyoutVideoIdInterface(messageType)
            }
        }

        // Playlists in the You tab use a separate generated message on 20.21+.
        SingularGeneratedExtensionFingerprint.let { fingerprint ->
            val match = fingerprint.matchOrNull()
            if (match != null) {
                val messageType = fingerprint.instructionMatches[1]
                    .instruction.getReference<FieldReference>()!!.type
                addFlyoutVideoIdInterface(messageType)
            }
        }

        FeedFlyoutBufferObjectFingerprint.method.addInstruction(
            0,
            "invoke-static/range {p2 .. p2}, $EXTENSION_CLASS_DESCRIPTOR->extractFlyoutVideoId(Ljava/util/Map;)V",
        )

        FullHistoryFlyoutBufferObjectFingerprint.let {
            val index = it.instructionMatches[2].index
            val register = it.method.getInstruction<OneRegisterInstruction>(index).registerA
            it.method.addInstruction(
                index + 1,
                "invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->extractFlyoutVideoId(Ljava/lang/Object;)V",
            )
        }

        FeedFlyoutButtonsInitializerFingerprint.let {
            it.method.apply {
                val runnableIndex = it.instructionMatches.last().index
                val runnableRegister =
                    getInstruction<TwoRegisterInstruction>(runnableIndex).registerA
                addInstructions(
                    runnableIndex,
                    """
                        invoke-static {v$runnableRegister}, $EXTENSION_CLASS_DESCRIPTOR->replaceQueueButtonRunnable(Ljava/lang/Runnable;)Ljava/lang/Runnable;
                        move-result-object v$runnableRegister
                    """,
                )

                val textIndex = it.instructionMatches[5].index
                val enumRegister =
                    it.instructionMatches[1].instruction.let { instruction ->
                        (instruction as OneRegisterInstruction).registerA
                    }
                val textRegister = getInstruction<OneRegisterInstruction>(textIndex).registerA
                val freeRegister = findFreeRegister(textIndex, textRegister, enumRegister)
                val enumIntField =
                    it.instructionMatches[7].instruction.getReference<FieldReference>()!!
                val enumMethod =
                    it.instructionMatches[8].instruction.getReference<MethodReference>()!!

                addInstructions(
                    textIndex,
                    """
                        iget v$freeRegister, v$enumRegister, $enumIntField
                        invoke-static {v$freeRegister}, $enumMethod
                        move-result-object v$freeRegister
                        invoke-static {v$freeRegister, v$textRegister}, $EXTENSION_CLASS_DESCRIPTOR->setCurrentFlyoutButton(Ljava/lang/Enum;Ljava/lang/CharSequence;)V
                    """,
                )

                // Removes the right-side native queue badge
                instructions.withIndex()
                    .filter { (_, instruction) ->
                        instruction.opcode == Opcode.INVOKE_DIRECT &&
                                instruction.getReference<MethodReference>()?.returnType ==
                                "Landroid/graphics/drawable/Drawable;"
                    }
                    .map { (index, _) -> index + 1 }
                    .lastOrNull()
                    ?.let { resultIndex ->
                        val badgeRegister = getInstruction<OneRegisterInstruction>(resultIndex).registerA
                        addInstructions(
                            resultIndex + 1,
                            """
                                invoke-static {v$badgeRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideQueueFlyoutBadge(Landroid/graphics/drawable/Drawable;)Landroid/graphics/drawable/Drawable;
                                move-result-object v$badgeRegister
                            """,
                        )
                    }
            }
        }

        FeedFlyoutButtonsInitializerOnItemClickFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {p3}, $EXTENSION_CLASS_DESCRIPTOR->replaceQueueOnItemClick(I)Z
                move-result p2
                if-eqz p2, :original_click
                return-void
                :original_click
                nop
            """,
        )

        FeedBottomSheetFlyoutFingerprint.method.apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstruction(
                    index,
                    "invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->setQueueBottomSheetFlyout(Landroid/app/Dialog;)V",
                )
            }
        }

        // endregion

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

        setPlaylistDownloadButtonVisibilityFingerprint.matchOrNull()?.let { match ->
            match.method.apply {
                val insertIndex = match.instructionMatches.first().index + 2
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

        try {
            val settings = mutableListOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: HOOK_BUTTONS",
                "SETTINGS: HOOK_DOWNLOAD_ACTIONS",
                "SETTINGS: OVERRIDE_PLAY_NEXT_IN_QUEUE",
            )
            addPreference(
                settings.toTypedArray(),
                HOOK_DOWNLOAD_ACTIONS
            )
        } catch (e: Throwable) {
            // Settings not initialized (e.g. on v19.28)
        }

        // endregion

    }
}
