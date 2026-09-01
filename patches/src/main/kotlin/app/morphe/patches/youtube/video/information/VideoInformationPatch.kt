/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 * - Jav1x (https://github.com/Jav1x)
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
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.video.information

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableField
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.util.smali.toInstructions
import app.morphe.patches.shared.FIXED_RESOLUTION_STRING
import app.morphe.patches.shared.formatStreamModelToStringFingerprint
import app.morphe.patches.shared.mdxPlayerDirectorSetVideoStageFingerprint
import app.morphe.patches.shared.playbackStartParametersToStringFingerprint
import app.morphe.patches.shared.videoLengthFingerprint
import app.morphe.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE
import app.morphe.patches.youtube.utils.YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
import app.morphe.patches.youtube.utils.extension.Constants.SHARED_PATH
import app.morphe.patches.youtube.utils.extension.Constants.VIDEO_PATH
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_20_49_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_04_or_greater
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.videoEndFingerprint
import app.morphe.patches.youtube.utils.videoIdFingerprintShorts
import app.morphe.patches.youtube.video.playerresponse.Hook
import app.morphe.patches.youtube.video.playerresponse.addPlayerResponseMethodHook
import app.morphe.patches.youtube.video.playerresponse.playerResponseMethodHookPatch
import app.morphe.patches.youtube.video.quality.getPlaybackStartParametersConstructorFingerprint
import app.morphe.patches.youtube.video.videoid.hookBackgroundPlayVideoIdNoArgs
import app.morphe.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.morphe.patches.youtube.video.videoid.hookVideoId
import app.morphe.patches.youtube.video.videoid.videoIdPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.addStaticFieldToExtension
import app.morphe.util.cloneMutable
import app.morphe.util.findFieldFromToString
import app.morphe.util.findMethodFromToString
import app.morphe.util.findMethodOrThrow
import app.morphe.util.findMutableClassOrThrow
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodCall
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.fingerprint.originalMethodOrThrow
import app.morphe.util.getReference
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil
import app.morphe.patcher.methodCall as patcherMethodCall

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/VideoInformation;"

private const val EXO_PLAYER_TYPE = "Landroidx/media3/exoplayer/ExoPlayer;"

internal const val EXTENSION_EXOPLAYERIMPL_INTERFACE =
    $$"$$SHARED_PATH/VideoInformation$ExoPlayerImpl;"

internal const val EXTENSION_PLAYBACK_SPEED_MENU_INTERFACE =
    $$"$$SHARED_PATH/VideoInformation$PlaybackSpeedMenuInterface;"

private const val EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VideoQualityPatch;"

private const val EXTENSION_VIDEO_QUALITY_INTERFACE =
    $$"$$VIDEO_PATH/VideoQualityPatch$VideoQualityInterface;"

private const val EXTENSION_VIDEO_QUALITY_MENU_INTERFACE =
    $$"$$VIDEO_PATH/VideoQualityPatch$VideoQualityMenuInterface;"

private const val REGISTER_PLAYER_RESPONSE_MODEL = 8

private const val REGISTER_CHANNEL_ID = 0
private const val REGISTER_CHANNEL_NAME = 1
private const val REGISTER_VIDEO_ID = 2
private const val REGISTER_VIDEO_TITLE = 3
private const val REGISTER_VIDEO_LENGTH = 4

@Suppress("unused")
private const val REGISTER_VIDEO_LENGTH_DUMMY = 5
private const val REGISTER_VIDEO_IS_LIVE = 6

private lateinit var channelIdMethodCall: String
private lateinit var channelNameMethodCall: String
private lateinit var videoIdMethodCall: String
private lateinit var videoTitleMethodCall: String
private lateinit var videoLengthMethodCall: String
private lateinit var videoIsLiveMethodCall: String

private lateinit var videoInformationMethod: MutableMethod
private lateinit var backgroundVideoInformationMethod: MutableMethod
private lateinit var shortsVideoInformationMethod: MutableMethod

/**
 * Used in [videoEndFingerprint] and [mdxPlayerDirectorSetVideoStageFingerprint].
 * Since both classes are inherited from the same class,
 * [videoEndFingerprint] and [mdxPlayerDirectorSetVideoStageFingerprint] always have the same [seekSourceEnumType] and [seekSourceMethodName].
 */
private var seekSourceEnumType = ""
private var seekSourceMethodName = ""
private var seekRelativeSourceMethodName = ""
private var cloneSeekRelativeSourceMethod = false

private lateinit var playerConstructorMethod: MutableMethod
private var playerConstructorInsertIndex = -1

private lateinit var mdxConstructorMethod: MutableMethod
private var mdxConstructorInsertIndex = -1

private lateinit var videoTimeConstructorMethod: MutableMethod
private var videoTimeConstructorInsertIndex = 2

private lateinit var setPlaybackSpeedMethodReference: MethodReference

// Used by other patches.
internal lateinit var speedSelectionInsertMethod: MutableMethod
internal lateinit var videoEndMethod: MutableMethod

val videoInformationPatch = bytecodePatch(
    description = "videoInformationPatch",
) {
    dependsOn(
        playerResponseMethodHookPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        videoIdPatch
    )

    execute {
        fun cloneSeekRelativeSourceMethod(mutableClass: MutableClass) {
            if (!cloneSeekRelativeSourceMethod) return

            val methods = mutableClass.methods

            methods.find { method ->
                method.name == seekRelativeSourceMethodName
            }?.apply {
                methods.add(
                    cloneMutable(
                        returnType = "Z"
                    ).apply {
                        val lastIndex = implementation!!.instructions.lastIndex

                        removeInstruction(lastIndex)
                        addInstructions(
                            lastIndex, """
                            move-result p1
                            return p1
                            """
                        )
                    }
                )
            }
        }

        fun addSeekInterfaceMethods(
            targetClass: MutableClass,
            targetMethod: MutableMethod,
            seekMethodName: String,
            methodName: String,
            fieldMethodName: String,
            fieldName: String
        ) {
            targetMethod.apply {
                targetClass.methods.add(
                    ImmutableMethod(
                        definingClass,
                        fieldMethodName,
                        listOf(ImmutableMethodParameter("J", annotations, "time")),
                        "Z",
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            4, """
                                # first enum (field a) is SEEK_SOURCE_UNKNOWN
                                sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                                invoke-virtual {p0, p1, p2, v0}, $definingClass->$seekMethodName(J$seekSourceEnumType)Z
                                move-result p1
                                return p1
                                """.toInstructions(),
                            null,
                            null
                        )
                    ).toMutable()
                )

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0, p0, p1}, $definingClass->$fieldMethodName(J)Z
                        move-result v0
                        return v0
                        :ignore
                        const/4 v0, 0x0
                        return v0
                        """

                addStaticFieldToExtension(
                    EXTENSION_CLASS_DESCRIPTOR,
                    methodName,
                    fieldName,
                    definingClass,
                    smaliInstructions
                )
            }
        }

        fun Pair<String, Fingerprint>.getPlayerResponseInstruction(
            returnType: String,
            fromString: Boolean? = null
        ): String {
            methodOrThrow().apply {
                val startIndex = if (fromString == true)
                    matchOrThrow().stringMatches.first().index
                else
                    0
                val targetReference = getInstruction<ReferenceInstruction>(
                    indexOfFirstInstructionOrThrow(startIndex) {
                        val reference = getReference<MethodReference>()
                        (opcode == Opcode.INVOKE_INTERFACE_RANGE || opcode == Opcode.INVOKE_INTERFACE) &&
                                reference?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR &&
                                reference.returnType == returnType
                    }
                ).reference

                return "invoke-interface {v$REGISTER_PLAYER_RESPONSE_MODEL}, $targetReference"
            }
        }

        if (is_21_04_or_greater) {
            val playerClass = PlayerInitFingerprint.classDef
            val seekMethod = SeekFingerprint.match(playerClass).method
            val seekRelativeMethod = SeekRelativeFingerprint.match(playerClass).method

            playerClass.methods.first {
                it.name == "<init>"
            }.let {
                playerConstructorMethod = it
                playerConstructorInsertIndex = it.indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
                } + 1
            }

            onCreateHook(EXTENSION_CLASS_DESCRIPTOR, "initialize")

            seekMethod.apply {
                seekSourceEnumType = parameterTypes[1].toString()
                seekSourceMethodName = name
            }

            seekRelativeMethod.apply {
                seekRelativeSourceMethodName = name
                cloneSeekRelativeSourceMethod = returnType == "V"
            }

            cloneSeekRelativeSourceMethod(playerClass)

            addSeekInterfaceMethods(
                playerClass,
                seekMethod,
                seekMethod.name,
                "overrideVideoTime",
                "seekTo",
                "videoInformationClass"
            )
            addSeekInterfaceMethods(
                playerClass,
                seekRelativeMethod,
                seekRelativeMethod.name,
                "overrideVideoTimeRelative",
                "seekToRelative",
                "videoInformationClass"
            )

            seekMethod.apply {
                val literalIndex = indexOfFirstLiteralInstructionOrThrow(45368273L)
                val walkerIndex = indexOfFirstInstructionReversedOrThrow(literalIndex) {
                    val reference = getReference<MethodReference>()
                    (opcode == Opcode.INVOKE_VIRTUAL || opcode == Opcode.INVOKE_VIRTUAL_RANGE) &&
                            reference?.definingClass == definingClass &&
                            reference.parameterTypes.isEmpty() &&
                            reference.returnType == "V"
                }

                videoEndMethod = getWalkerMethod(walkerIndex)
            }
        } else videoEndFingerprint.methodOrThrow().apply {
            findMethodOrThrow(definingClass).let {
                playerConstructorMethod = it
                playerConstructorInsertIndex = it.indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
                } + 1
            }

            // hook the player controller for use through extension
            onCreateHook(EXTENSION_CLASS_DESCRIPTOR, "initialize")

            seekSourceEnumType = parameterTypes[1].toString()
            seekSourceMethodName = name

            seekRelativeFingerprint.methodOrThrow(videoEndFingerprint).also { method ->
                seekRelativeSourceMethodName = method.name
                cloneSeekRelativeSourceMethod = method.returnType == "V"
            }

            cloneSeekRelativeSourceMethod(videoEndFingerprint.mutableClassOrThrow())

            // Create extension interface methods.
            addSeekInterfaceMethods(
                videoEndFingerprint.mutableClassOrThrow(),
                this,
                seekSourceMethodName,
                "overrideVideoTime",
                "seekTo",
                "videoInformationClass"
            )
            addSeekInterfaceMethods(
                seekRelativeFingerprint.mutableClassOrThrow(),
                this,
                seekRelativeSourceMethodName,
                "overrideVideoTimeRelative",
                "seekToRelative",
                "videoInformationClass"
            )

            val literalIndex = indexOfFirstLiteralInstructionOrThrow(45368273L)
            val walkerIndex = indexOfFirstInstructionReversedOrThrow(literalIndex) {
                val reference = getReference<MethodReference>()
                (opcode == Opcode.INVOKE_VIRTUAL || opcode == Opcode.INVOKE_VIRTUAL_RANGE) &&
                        reference?.definingClass == definingClass &&
                        reference.parameterTypes.isEmpty() &&
                        reference.returnType == "V"
            }

            videoEndMethod = getWalkerMethod(walkerIndex)
        }

        mdxPlayerDirectorSetVideoStageFingerprint.methodOrThrow().apply {
            findMethodOrThrow(definingClass).let {
                mdxConstructorMethod = it
                mdxConstructorInsertIndex = it.indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
                } + 1
            }

            // hook the MDX director for use through extension
            onCreateHookMdx(EXTENSION_CLASS_DESCRIPTOR, "initializeMdx")

            cloneSeekRelativeSourceMethod(mdxPlayerDirectorSetVideoStageFingerprint.mutableClassOrThrow())

            // Create extension interface methods.
            addSeekInterfaceMethods(
                mdxPlayerDirectorSetVideoStageFingerprint.mutableClassOrThrow(),
                this,
                seekSourceMethodName,
                "overrideMDXVideoTime",
                "seekTo",
                "videoInformationMDXClass"
            )
            addSeekInterfaceMethods(
                mdxPlayerDirectorSetVideoStageFingerprint.mutableClassOrThrow(),
                this,
                seekRelativeSourceMethodName,
                "overrideMDXVideoTimeRelative",
                "seekToRelative",
                "videoInformationMDXClass"
            )
        }

        if (is_21_04_or_greater) {
            val playerClass = PlayerInitFingerprint.classDef

            ModernChannelInformationFingerprint.let {
                val matches = it.matchAll(2..3)
                val playerResponseType = matches.first().method.parameterTypes.first().toString()
                val channelIdMethodCall = getModernChannelIdFingerprint(playerResponseType)
                    .instructionMatches.first().instruction.getReference<MethodReference>()!!
                val channelNameMethodCall = getModernChannelNameFingerprint(playerResponseType)
                    .instructionMatches.last().instruction.getReference<MethodReference>()!!

                it.classDef.apply {
                    val helperMethod = ImmutableMethod(
                        type,
                        "setChannelInformation",
                        listOf(ImmutableMethodParameter(playerResponseType, annotations, null)),
                        "V",
                        AccessFlags.PRIVATE or AccessFlags.FINAL,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            3,
                            """
                                invoke-interface { p1 }, $channelIdMethodCall
                                move-result-object v0
                                invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->setChannelId(Ljava/lang/String;)V

                                invoke-interface { p1 }, $channelNameMethodCall
                                move-result-object v0
                                invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->setChannelName(Ljava/lang/String;)V

                                return-void
                            """.toInstructions(),
                            null,
                            null,
                        ),
                    ).toMutable()

                    methods.add(helperMethod)
                    matches.forEach { match ->
                        match.method.addInstruction(
                            0,
                            "invoke-direct { p0, p1 }, $helperMethod",
                        )
                    }
                }
            }

            VideoLengthFingerprint.match(CreateVideoPlayerSeekbarFingerprint.originalClassDef).let {
                it.method.apply {
                    val targetIndex = it.instructionMatches.last().index
                    val registerIndex = targetIndex - 2
                    val register = getInstruction<OneRegisterInstruction>(registerIndex).registerA
                    addInstruction(
                        targetIndex,
                        "invoke-static { v$register, v${register + 1} }, " +
                                "$EXTENSION_CLASS_DESCRIPTOR->setVideoLength(J)V",
                    )
                    addInstruction(
                        targetIndex + 1,
                        "invoke-static {}, ${playerClass.type}->patch_dispatchVideoInformation()V",
                    )
                }
            }

            val modernVideoInformationMethod = playerClass.getModernVideoInformationMethod(
                "patch_setVideoInformation",
            )
            val modernShortsVideoInformationMethod = playerClass.getModernVideoInformationMethod(
                "patch_setShortsVideoInformation",
            )
            val modernBackgroundVideoInformationMethod = playerClass.getModernVideoInformationMethod(
                "patch_setBackgroundVideoInformation",
            )
            val modernVideoInformationDispatcherMethod =
                playerClass.getModernVideoInformationDispatcherMethod()
            playerClass.methods.addAll(
                listOf(
                    modernVideoInformationMethod,
                    modernShortsVideoInformationMethod,
                    modernBackgroundVideoInformationMethod,
                    modernVideoInformationDispatcherMethod,
                )
            )
            videoInformationMethod = modernVideoInformationMethod
            shortsVideoInformationMethod = modernShortsVideoInformationMethod
            backgroundVideoInformationMethod = modernBackgroundVideoInformationMethod

            // General video-information callbacks are shared by regular videos and Shorts. The
            // dispatcher retains the old Shorts-only scope through modern video-ID state.
            hookBackgroundPlayVideoIdNoArgs(
                "${playerClass.type}->patch_setBackgroundVideoInformation()V",
            )
        } else {
            /**
             * Set current video information
             */
            channelIdMethodCall =
                channelIdFingerprint.getPlayerResponseInstruction("Ljava/lang/String;")
            channelNameMethodCall =
                channelNameFingerprint.getPlayerResponseInstruction("Ljava/lang/String;", true)
            videoIdMethodCall = videoIdFingerprint.getPlayerResponseInstruction("Ljava/lang/String;")
            videoTitleMethodCall =
                videoTitleFingerprint.getPlayerResponseInstruction("Ljava/lang/String;")
            videoLengthMethodCall = videoLengthFingerprint.getPlayerResponseInstruction("J")
            videoIsLiveMethodCall = channelIdFingerprint.getPlayerResponseInstruction("Z")

            playbackInitializationFingerprint.matchOrThrow().let {
                it.method.apply {
                    val targetIndex = indexOfPlayerResponseModelDirectInstruction(this) + 1
                    val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstruction(
                        targetIndex + 1,
                        "invoke-direct {p0, v$targetRegister}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                    )

                    videoInformationMethod = getVideoInformationMethod()
                    it.classDef.methods.add(videoInformationMethod)

                    hookVideoInformation("$EXTENSION_CLASS_DESCRIPTOR->setVideoInformation(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
                }
            }

            videoIdFingerprintBackgroundPlay.matchOrThrow().let {
                it.method.apply {
                    val targetIndex = indexOfPlayerResponseModelInterfaceInstruction(this)
                    val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                    addInstruction(
                        targetIndex,
                        "invoke-direct {p0, v$targetRegister}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                    )

                    backgroundVideoInformationMethod = getVideoInformationMethod()
                    it.classDef.methods.add(backgroundVideoInformationMethod)
                }
            }

            videoIdFingerprintShorts.matchOrThrow().let {
                it.method.apply {
                    val targetIndex = indexOfPlayerResponseModelInterfaceInstruction(this)
                    val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                    addInstruction(
                        targetIndex,
                        "invoke-direct {p0, v$targetRegister}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                    )

                    shortsVideoInformationMethod = getVideoInformationMethod()
                    it.classDef.methods.add(shortsVideoInformationMethod)
                }
            }
        }

        /**
         * Set current video time method
         */
        playerControllerSetTimeReferenceFingerprint.matchOrThrow().let {
            videoTimeConstructorMethod =
                it.getWalkerMethod(it.instructionMatches.first().index)
        }

        /**
         * Store receiver (p0) of the time-update method so VOT can use it for volume when playbackSpeedClass is null.
         */
        videoTimeConstructorMethod.addInstruction(
            videoTimeConstructorInsertIndex++,
            "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->setTimeUpdateReceiver(Ljava/lang/Object;)V"
        )
        /**
         * Set current video time
         */
        videoTimeHook(EXTENSION_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Set current video id
         */
        hookVideoId("$EXTENSION_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")
        hookPlayerResponseVideoId(
            "$EXTENSION_CLASS_DESCRIPTOR->setPlayerResponseVideoId(Ljava/lang/String;Z)V"
        )
        // Call before any other video id hooks,
        // so they can use VideoInformation and check if the video id is for a Short.
        addPlayerResponseMethodHook(
            Hook.ProtoBufferParameterBeforeVideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;"
            )
        )

        /**
         * Hook current playback speed
         */
        val playbackSpeedItemClickFingerprint = if (is_21_04_or_greater) {
            ModernPlaybackSpeedOnItemClickFingerprint.match(
                OnPlaybackSpeedItemClickParentFingerprint.classDef,
            )
        } else {
            onPlaybackSpeedItemClickFingerprint.matchOrThrow()
        }

        playbackSpeedItemClickFingerprint.let {
            it.method.apply {
                speedSelectionInsertMethod = this
                val speedSelectionValueInstructionIndex =
                    indexOfFirstInstructionOrThrow(Opcode.IGET)

                val setPlaybackSpeedMethodReferenceIndex =
                    indexOfFirstInstructionOrThrow(speedSelectionValueInstructionIndex) {
                        val reference = getReference<MethodReference>()
                        reference?.parameterTypes?.size == 1 && reference.parameterTypes.first() == "F"
                    }
                setPlaybackSpeedMethodReference =
                    getInstruction<ReferenceInstruction>(setPlaybackSpeedMethodReferenceIndex).reference as MethodReference

                val setPlaybackSpeedContainerClassFieldReference =
                    getInstruction<ReferenceInstruction>(
                        indexOfFirstInstructionOrThrow(Opcode.IF_EQZ) - 1
                    ).reference as FieldReference

                val setPlaybackSpeedContainerClassFieldReferenceClassType: ClassDef =
                    if (is_20_49_or_greater) {
                        var fieldReferenceType: ClassDef? = null
                        classDefForEach { def ->
                            if (def.interfaces.contains(setPlaybackSpeedContainerClassFieldReference.type)) {
                                if (fieldReferenceType != null) {
                                    throw PatchException("Found more than one playback speed interface: $def")
                                }
                                fieldReferenceType = def
                            }
                        }
                        fieldReferenceType
                            ?: throw PatchException("Failed to find playback speed interface implementation")
                    } else {
                        classDefBy(setPlaybackSpeedContainerClassFieldReference.type)
                    }

                val setPlaybackSpeedClassFieldReference =
                    getInstruction<ReferenceInstruction>(
                        indexOfFirstInstructionOrThrow(speedSelectionValueInstructionIndex) {
                            getReference<FieldReference>()?.type?.startsWith("L") == true
                        }
                    ).reference as FieldReference
                // add override playback speed method
                it.classDef.methods.add(
                    ImmutableMethod(
                        definingClass,
                        "overridePlaybackSpeed",
                        listOf(ImmutableMethodParameter("F", annotations, null)),
                        "V",
                        AccessFlags.PUBLIC or AccessFlags.PUBLIC,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            4, """
                                # Check if the playback speed is not auto (-2.0f)
                                const/4 v0, 0x0
                                cmpg-float v0, v3, v0
                                if-lez v0, :ignore
                                
                                # Get the container class field.
                                iget-object v0, v2, $setPlaybackSpeedContainerClassFieldReference  

                                # For some reason, in YouTube 19.44.39 this value is sometimes null.
                                if-eqz v0, :ignore

                                # Required cast for 20.49+
                                check-cast v0, $setPlaybackSpeedContainerClassFieldReferenceClassType

                                # Get the field from its class.
                                iget-object v1, v0, $setPlaybackSpeedClassFieldReference
                                
                                # Invoke setPlaybackSpeed on that class.
                                invoke-virtual {v1, v3}, $setPlaybackSpeedMethodReference

                                :ignore
                                return-void
                                """.toInstructions(), null, null
                        )
                    ).toMutable()
                )

                // set current playback speed
                val walkerMethod = getWalkerMethod(setPlaybackSpeedMethodReferenceIndex)
                walkerMethod.apply {
                    addInstruction(
                        this.implementation!!.instructions.size - 1,
                        "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->setPlaybackSpeed(F)V"
                    )
                }
            }
        }

        if (is_21_04_or_greater) {
            InitializePlaybackSpeedValuesFingerprint.let {
                it.classDef.apply {
                    // Add interface and helper methods to allow extension code to call obfuscated methods.
                    interfaces.add(EXTENSION_PLAYBACK_SPEED_MENU_INTERFACE)

                    // Player controller field does not exist in YouTube 20.x, add the field.
                    val playerControllerField: MutableField

                    methods.first { method ->
                        MethodUtil.isConstructor(method)
                    }.apply {
                        val playerControllerClass = setPlaybackSpeedMethodReference.definingClass
                        val playerControllerClassIndex = parameterTypes.indexOfFirst { parameterType ->
                            parameterType == playerControllerClass
                        }
                        if (playerControllerClassIndex < 0) {
                            throw PatchException("Could not find player controller index")
                        }
                        playerControllerField = ImmutableField(
                            type,
                            "patch_playerController",
                            playerControllerClass,
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            null,
                        ).toMutable()

                        instanceFields.add(playerControllerField)

                        val playerControllerClassRegister =
                            implementation!!.registerCount - parameters.size + playerControllerClassIndex

                        addInstructions(
                            2,
                            """
                            invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->setPlaybackSpeedMenu($EXTENSION_PLAYBACK_SPEED_MENU_INTERFACE)V                            
                            iput-object v$playerControllerClassRegister, p0, $playerControllerField
                        """
                        )
                    }

                    methods.add(
                        ImmutableMethod(
                            type,
                            "patch_setSpeed",
                            listOf(
                                ImmutableMethodParameter("F", null, null)
                            ),
                            "V",
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(3),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                iget-object v0, p0, $playerControllerField
                                
                                # Check if the player controller class is null.
                                if-eqz v0, :ignore
                                invoke-virtual { v0, p1 }, $setPlaybackSpeedMethodReference
                                
                                :ignore
                                return-void
                            """
                            )
                        }
                    )
                }
            }
        }

        if (!is_21_04_or_greater) {
            videoIdFingerprintShorts.matchOrThrow().let {
                it.method.apply {
                    val shortsPlaybackSpeedClassField = it.classDef.fields.find { field ->
                        field.type == setPlaybackSpeedMethodReference.definingClass
                    } ?: throw PatchException("Failed to find hook field")

                    val smaliInstructions =
                        """
                            if-eqz v0, :ignore
                            invoke-virtual {v0, p0}, $definingClass->overridePlaybackSpeed(F)V
                            :ignore
                            return-void
                            """

                    addStaticFieldToExtension(
                        EXTENSION_CLASS_DESCRIPTOR,
                        "overridePlaybackSpeed",
                        "playbackSpeedShortsClass",
                        definingClass,
                        smaliInstructions
                    )

                    // add override playback speed method
                    it.classDef.methods.add(
                        ImmutableMethod(
                            definingClass,
                            "overridePlaybackSpeed",
                            listOf(ImmutableMethodParameter("F", annotations, null)),
                            "V",
                            AccessFlags.PUBLIC or AccessFlags.PUBLIC,
                            annotations,
                            null,
                            ImmutableMethodImplementation(
                                3, """
                                    # Check if the playback speed is not auto (-2.0f)
                                    const/4 v0, 0x0
                                    cmpg-float v0, v2, v0
                                    if-lez v0, :ignore

                                    # Get the container class field.
                                    iget-object v0, v1, $shortsPlaybackSpeedClassField

                                    # For some reason, in YouTube 19.44.39 this value is sometimes null.
                                    if-eqz v0, :ignore

                                    # Invoke setPlaybackSpeed on that class.
                                    invoke-virtual {v0, v2}, $setPlaybackSpeedMethodReference

                                    :ignore
                                    return-void
                                    """.toInstructions(), null, null
                            )
                        ).toMutable()
                    )
                }
            }
        }

        if (!is_21_04_or_greater) {
            playbackSpeedClassFingerprint.methodOrThrow().apply {
                val index = indexOfFirstInstructionOrThrow(Opcode.RETURN_OBJECT)
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                val playbackSpeedClass = this.returnType

                // set playback speed class
                addInstructionsAtControlFlowLabel(
                    index,
                    "sput-object v$register, $EXTENSION_CLASS_DESCRIPTOR->playbackSpeedClass:$playbackSpeedClass"
                )

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0, p0}, $playbackSpeedClass->overridePlaybackSpeed(F)V
                        return-void
                        :ignore
                        nop
                        """

                addStaticFieldToExtension(
                    EXTENSION_CLASS_DESCRIPTOR,
                    "overridePlaybackSpeed",
                    "playbackSpeedClass",
                    playbackSpeedClass,
                    smaliInstructions,
                    false
                ).apply {
                    val jumpIndex = indexOfFirstInstructionOrThrow(Opcode.NOP)

                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isPlayerInitialized()Z
                            move-result v0
                            if-eqz v0, :ignore
                            """, ExternalLabel("ignore", getInstruction(jumpIndex))
                    )
                }

                hookBackgroundPlayVideoInformation("$EXTENSION_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
            }
        }

        // region ExoPlayerImpl.

        val playbackParametersType = PlaybackParametersToStringFingerprint.classDef.type
        val setPlaybackParametersFingerprint = getPlaybackParametersSetterFingerprint(playbackParametersType)
        val playerInterfaceType = classDefBy(EXO_PLAYER_TYPE).interfaces.first()
        // Resolve this through the stable media3 interface because the concrete player implementation is obfuscated.
        val setPlayWhenReadyMethod = Fingerprint(
            definingClass = playerInterfaceType,
            returnType = "V",
            parameters = listOf("Z"),
        ).method

        // for patch_setPlaybackParameters helper method to call setPlaybackParameters(PlaybackParameters p1).
        val setPlaybackParametersMethod = setPlaybackParametersFingerprint.method

        // A reference to the setPlaybackParameters implementation, to call from the helper method.
        val setPlaybackParametersReference =
            "${setPlaybackParametersMethod.definingClass}->${setPlaybackParametersMethod.name}($playbackParametersType)V"

        // for {androidx.media3.common.PlaybackParameters.speed} field.
        // The toString() method reads the speed field before the pitch field.
        val playbackParametersSpeedField = PlaybackParametersToStringFingerprint
            .instructionMatches.first().getInstruction<ReferenceInstruction>().getReference<FieldReference>()!!

        // The PlaybackParameters type and primary constructor with 2 arguments (speed, pitch).
        val playbackParametersConstructorReference = "$playbackParametersType-><init>(FF)V"

        // Pitch is obtained from this Extension and force set.
        // Need to construct new PlaybackParameters instance as it has final fields.
        setPlaybackParametersMethod.addInstructions(
            0,
            """
                iget v0, p1, $playbackParametersSpeedField
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getPlaybackAudioPitch()F
                move-result v1
                new-instance p1, $playbackParametersType
                invoke-direct {p1, v0, v1}, $playbackParametersConstructorReference
            """
        )

        // Capture the ExoPlayerImpl reference at its init constructor (only 1 yet)
        // Extension is initialized (Application.onCreate) before starting to play any video.
        // This is required for the extension's playback parameter and state bridge methods.
        Fingerprint(
            classFingerprint = setPlaybackParametersFingerprint,
            name = "<init>",
            filters = listOf(
                patcherMethodCall(
                    opcode = Opcode.INVOKE_DIRECT,
                    name = "<init>"
                )
            )
        ).matchAll().forEach {
            val firstInstructionMatch = it.instructionMatches.first()
            val register = firstInstructionMatch.getInstruction<FiveRegisterInstruction>().registerC
            it.method.addInstruction(
                firstInstructionMatch.index + 1,
                "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->" +
                        "initializeExoPlayerImpl($EXTENSION_EXOPLAYERIMPL_INTERFACE)V"
            )
        }

        setPlaybackParametersFingerprint.classDef.apply {
            // Add interface and helper methods to allow extension code to directly control ExoPlayer playback.
            interfaces.add(EXTENSION_EXOPLAYERIMPL_INTERFACE)

            methods.add(
                ImmutableMethod(
                    type,
                    "patch_setPlaybackParameters",
                    listOf(
                        ImmutableMethodParameter("F", null, null),
                        ImmutableMethodParameter("F", null, null)
                    ),
                    "V",
                    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                    null,
                    null,
                    MutableMethodImplementation(4),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                            new-instance v0, $playbackParametersType
                            invoke-direct { v0, p1, p2 }, $playbackParametersConstructorReference
                            invoke-virtual { p0, v0 }, $setPlaybackParametersReference
                            return-void
                        """
                    )
                }
            )

            methods.add(
                ImmutableMethod(
                    type,
                    "patch_setPlayWhenReady",
                    listOf(ImmutableMethodParameter("Z", null, null)),
                    "V",
                    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                    null,
                    null,
                    MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                            invoke-interface { p0, p1 }, ${setPlayWhenReadyMethod.definingClass}->${setPlayWhenReadyMethod.name}(Z)V
                            return-void
                        """
                    )
                }
            )
        }

        // endregion.

        if (is_21_04_or_greater) {
            onCreateHook(EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR, "newVideoStarted")
            val modernVideoQualityClassType = VideoQualityFingerprint.classDef.type

            VideoQualityFingerprint.let {
                val (qualityNameField, resolutionField) = with(it.method) {
                    val qualityNameIndex = indexOfFirstInstructionOrThrow {
                        val reference = getReference<FieldReference>()
                        opcode == Opcode.IPUT_OBJECT &&
                                reference?.type == "Ljava/lang/String;" &&
                                reference.definingClass == definingClass
                    }
                    val resolutionIndex = indexOfFirstInstructionOrThrow {
                        val reference = getReference<FieldReference>()
                        opcode == Opcode.IPUT &&
                                reference?.type == "I" &&
                                reference.definingClass == definingClass
                    }
                    val qualityNameField =
                        getInstruction<ReferenceInstruction>(qualityNameIndex).reference
                    val resolutionField =
                        getInstruction<ReferenceInstruction>(resolutionIndex).reference

                    addInstructions(
                        0,
                        """
                            invoke-static { p3, p1 }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->fixVideoQualityResolution(Ljava/lang/String;I)I
                            move-result p1
                            """
                    )

                    Pair(
                        qualityNameField,
                        resolutionField,
                    )
                }

                it.classDef.apply {
                    interfaces.add(EXTENSION_VIDEO_QUALITY_INTERFACE)

                    methods.add(
                        ImmutableMethod(
                            type,
                            "patch_getQualityName",
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
                                    iget-object v0, p0, $qualityNameField
                                    return-object v0
                                    """
                            )
                        }
                    )

                    methods.add(
                        ImmutableMethod(
                            type,
                            "patch_getResolution",
                            listOf(),
                            "I",
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(2),
                        ).toMutable().apply {
                            addInstructions(
                                0,
                                """
                                    iget v0, p0, $resolutionField
                                    return v0
                                    """
                            )
                        }
                    )
                }
            }

            ModernSetVideoQualityFingerprint.let {
                val onQualityItemClickField =
                    it.instructionMatches[0].instruction.getReference<FieldReference>()!!
                val setQualityField =
                    it.instructionMatches[1].instruction.getReference<FieldReference>()!!

                findMutableClassOrThrow(setQualityField.type).apply {
                    interfaces.add(EXTENSION_VIDEO_QUALITY_MENU_INTERFACE)
                    methods.add(
                        ImmutableMethod(
                            type,
                            "patch_setQuality",
                            listOf(
                                ImmutableMethodParameter(
                                    EXTENSION_VIDEO_QUALITY_INTERFACE,
                                    null,
                                    null,
                                )
                            ),
                            "V",
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(2),
                        ).toMutable().apply {
                            val setQualityMenuIndexMethod = methods.single { method ->
                                method.parameterTypes.firstOrNull() == modernVideoQualityClassType
                            }

                            addInstructions(
                                0,
                                """
                                    check-cast p1, $modernVideoQualityClassType
                                    invoke-virtual { p0, p1 }, $setQualityMenuIndexMethod
                                    return-void
                                    """
                            )
                        }
                    )
                }

                VideoQualitySetterFingerprint.method.addInstructions(
                    0,
                    """
                        iget-object v0, p0, $onQualityItemClickField
                        iget-object v0, v0, $setQualityField

                        invoke-static { p1, v0, p2 }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQuality([$EXTENSION_VIDEO_QUALITY_INTERFACE${EXTENSION_VIDEO_QUALITY_MENU_INTERFACE}I)I
                        move-result p2
                        """
                )
            }
        }

        /**
         * Hook current video quality for versions before YouTube 21.04.
         */
        if (!is_21_04_or_greater) {
            onCreateHook(EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR, "newVideoStarted")

            videoQualityFingerprint.matchOrThrow().let {
                // Fix bad data used by YouTube.
                val (qualityNameField, resolutionField) = with(it.method) {
                    val qualityNameIndex = indexOfVideoQualityNameFieldInstruction(this)
                    val resolutionIndex = indexOfVideoQualityResolutionFieldInstruction(this)
                    val qualityNameReference =
                        getInstruction<ReferenceInstruction>(qualityNameIndex).reference
                    val resolutionReference =
                        getInstruction<ReferenceInstruction>(resolutionIndex).reference
                    val qualityNameRegister =
                        getInstruction<TwoRegisterInstruction>(qualityNameIndex).registerA
                    val resolutionRegister =
                        getInstruction<TwoRegisterInstruction>(resolutionIndex).registerA

                    addInstructions(
                        0, """
                        invoke-static { v$qualityNameRegister, v$resolutionRegister }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->fixVideoQualityResolution(Ljava/lang/String;I)I
                        move-result v$resolutionRegister
                        """
                    )

                    Pair(
                        qualityNameReference,
                        resolutionReference
                    )
                }

                // Add methods to access obfuscated quality fields.
                it.classDef.apply {
                    // The legacy quality array keeps its concrete descriptor at the injection point.
                    // Expose its elements through the stable extension interface after the call.
                    interfaces.add(EXTENSION_VIDEO_QUALITY_INTERFACE)

                    methods.add(
                        ImmutableMethod(
                            type,
                            "patch_getQualityName",
                            listOf(),
                            "Ljava/lang/String;",
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(2),
                        ).toMutable().apply {
                            addInstructions(
                                0, """
                                iget-object v0, p0, $qualityNameField
                                return-object v0
                                """
                            )
                        }
                    )

                    methods.add(
                        ImmutableMethod(
                            type,
                            "patch_getResolution",
                            listOf(),
                            "I",
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(2),
                        ).toMutable().apply {
                            addInstructions(
                                0, """
                                iget v0, p0, $resolutionField
                                return v0
                                """
                            )
                        }
                    )
                }
            }

            val formatStreamFpsReference = formatStreamingModelQualityLabelBuilderFingerprint
                .matchOrThrow()
                .let {
                    with(it.method) {
                        val stringIndex = it.stringMatches.first().index
                        val formatStreamIndex = indexOfFirstInstructionReversedOrThrow(stringIndex) {
                            val reference = getReference<MethodReference>()
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    reference?.definingClass == YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE &&
                                    reference.parameterTypes.isEmpty() &&
                                    reference.returnType == "I"
                        }
                        getInstruction<ReferenceInstruction>(formatStreamIndex).reference
                    }
                }

            val formatStreamQualityNameReference = formatStreamingModelQualityLabelBuilderFingerprint
                .methodOrThrow()
                .methodCall()

            val formatStreamITagReference =
                formatStreamModelToStringFingerprint.originalMethodOrThrow()
                    .findMethodFromToString("FormatStream(itag=")
                    .methodCall()

            val formatStreamResolutionReference =
                availableVideoFormatsFingerprint.matchOrThrow(
                    formatStreamModelBuilderFingerprint
                ).let {
                    with(it.method) {
                        val formatStreamIndex = it.instructionMatches.first().index + 1
                        val formatStreamResolutionReference =
                            getInstruction<ReferenceInstruction>(formatStreamIndex).reference as MethodReference

                        addInstructions(
                            0,
                            "invoke-static { p0 }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoFormat(Ljava/util/List;)V"
                        )

                        formatStreamResolutionReference
                    }
                }

            mapOf(
                formatStreamFpsReference to "patch_getFps",
                formatStreamITagReference to "patch_getITag",
                formatStreamResolutionReference to "patch_getResolution",
                formatStreamQualityNameReference to "patch_getQualityName"
            ).forEach { (reference, methodName) ->
                val returnTypeIsInteger = reference.toString().endsWith("I")
                val returnType = if (returnTypeIsInteger) "I" else "Ljava/lang/String;"
                val smaliInstructions = if (returnTypeIsInteger)
                    """
                    invoke-virtual { p0 }, $reference
                    move-result v0
                    return v0
                """
                else
                    """
                    invoke-virtual { p0 }, $reference
                    move-result-object v0
                    return-object v0
                """
                findMutableClassOrThrow(YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE)
                    .methods.add(
                        ImmutableMethod(
                            YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE,
                            methodName,
                            listOf(),
                            returnType,
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(2),
                        ).toMutable().apply {
                            addInstructions(
                                0,
                                smaliInstructions
                            )
                        }
                    )
            }

            initFormatStreamFingerprint.methodOrThrow(initFormatStreamParentFingerprint)
                .apply {
                    val preferredFormatStreamIndex =
                        indexOfPreferredFormatStreamInstruction(this)
                    val preferredFormatStreamReference =
                        getInstruction<ReferenceInstruction>(preferredFormatStreamIndex).reference
                    val preferredFormatStreamInstruction =
                        getInstruction<TwoRegisterInstruction>(preferredFormatStreamIndex)
                    val preferredFormatStreamRegister =
                        preferredFormatStreamInstruction.registerA
                    val definingClassRegister =
                        preferredFormatStreamInstruction.registerB

                    addInstructions(
                        preferredFormatStreamIndex + 1, """
                        invoke-static { v$preferredFormatStreamRegister }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->getVideoFormat($YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE)$YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE
                        move-result-object v$preferredFormatStreamRegister
                        iput-object v$preferredFormatStreamRegister, v$definingClassRegister, $preferredFormatStreamReference
                        """
                    )
                }

            val playbackStartParametersToStringMethod =
                playbackStartParametersToStringFingerprint.originalMethodOrThrow()
            val initialResolutionField = playbackStartParametersToStringMethod
                .findFieldFromToString(FIXED_RESOLUTION_STRING)
            val playbackStartParametersClass =
                findMutableClassOrThrow(playbackStartParametersToStringMethod.definingClass)

            getPlaybackStartParametersConstructorFingerprint(initialResolutionField)
                .match(playbackStartParametersClass)
                .let { match ->
                    match.method.apply {
                        val index = match.instructionMatches.first().index
                        val register = getInstruction<TwoRegisterInstruction>(index).registerA

                        addInstructions(
                            index, $$"""
                            invoke-static {v$$register}, $$EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->getInitialVideoQuality(Lj$/util/Optional;)Lj$/util/Optional;
                            move-result-object v$$register
                            """
                        )
                    }
                }

            videoQualityArrayFingerprint.matchOrThrow(formatStreamModelBuilderFingerprint).let {
                it.method.apply {
                    val index = it.instructionMatches.first().index
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructionsAtControlFlowLabel(
                        index, """
                        invoke-static { v$register }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->removeLowFpsVideoQualities(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$register
                        """
                    )
                }
            }

            videoQualityListFingerprint.matchOrThrow().let {
                val classDef = it.classDef
                it.method.apply {
                    classDef.interfaces.add(EXTENSION_VIDEO_QUALITY_MENU_INTERFACE)

                    classDef.methods.add(
                        ImmutableMethod(
                            definingClass,
                            "patch_setQuality",
                            listOf(
                                ImmutableMethodParameter(
                                    EXTENSION_VIDEO_QUALITY_INTERFACE,
                                    annotations,
                                    null
                                )
                            ),
                            "V",
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(2),
                        ).toMutable().apply {
                            val setQualityMenuIndexMethod = classDef.methods.single { method ->
                                method.parameterTypes.firstOrNull() == YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
                            }

                            addInstructions(
                                0,
                                """
                                check-cast p1, $YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
                                invoke-virtual { p0, p1 }, $setQualityMenuIndexMethod
                                return-void
                                """
                            )
                        }
                    )
                    val interfaceIndex = it.instructionMatches.first().index
                    val listRegister =
                        getInstruction<FiveRegisterInstruction>(interfaceIndex).registerD
                    val indexRegister =
                        getInstruction<FiveRegisterInstruction>(interfaceIndex).registerE

                    addInstructions(
                        interfaceIndex, """
                        invoke-static { v$listRegister, p0, v$indexRegister }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQuality([$YOUTUBE_VIDEO_QUALITY_CLASS_TYPE${EXTENSION_VIDEO_QUALITY_MENU_INTERFACE}I)I
                        move-result v$indexRegister
                        """
                    )
                }
            }

            videoQualitySetterFingerprint.matchOrThrow().let {
                it.method.apply {
                    val textIndex = it.instructionMatches.last().index
                    val textRegister = getInstruction<TwoRegisterInstruction>(textIndex).registerA

                    addInstruction(
                        textIndex + 1,
                        "invoke-static {v$textRegister}, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->updateQualityString(Ljava/lang/String;)V"
                    )
                }
            }
        }
    }
}

private fun MutableMethod.getVideoInformationMethod(): MutableMethod =
    ImmutableMethod(
        definingClass,
        "setVideoInformation",
        listOf(
            ImmutableMethodParameter(
                PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR,
                annotations,
                null
            )
        ),
        "V",
        AccessFlags.PRIVATE or AccessFlags.FINAL,
        annotations,
        null,
        ImmutableMethodImplementation(
            REGISTER_PLAYER_RESPONSE_MODEL + 1, """
                $channelIdMethodCall
                move-result-object v$REGISTER_CHANNEL_ID
                $channelNameMethodCall
                move-result-object v$REGISTER_CHANNEL_NAME
                $videoIdMethodCall
                move-result-object v$REGISTER_VIDEO_ID
                $videoTitleMethodCall
                move-result-object v$REGISTER_VIDEO_TITLE
                $videoLengthMethodCall
                move-result-wide v$REGISTER_VIDEO_LENGTH
                $videoIsLiveMethodCall
                move-result v$REGISTER_VIDEO_IS_LIVE
                return-void
                """.toInstructions(),
            null,
            null
        )
    ).toMutable()

private fun MutableClass.getModernVideoInformationMethod(methodName: String): MutableMethod =
    ImmutableMethod(
        type,
        methodName,
        emptyList(),
        "V",
        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value,
        null,
        null,
        ImmutableMethodImplementation(
            7,
            """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getChannelId()Ljava/lang/String;
                move-result-object v$REGISTER_CHANNEL_ID
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getChannelName()Ljava/lang/String;
                move-result-object v$REGISTER_CHANNEL_NAME
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getVideoId()Ljava/lang/String;
                move-result-object v$REGISTER_VIDEO_ID
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getVideoTitle()Ljava/lang/String;
                move-result-object v$REGISTER_VIDEO_TITLE
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getVideoLength()J
                move-result-wide v$REGISTER_VIDEO_LENGTH
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getLiveStreamState()Z
                move-result v$REGISTER_VIDEO_IS_LIVE
                invoke-static/range { v$REGISTER_CHANNEL_ID .. v$REGISTER_VIDEO_IS_LIVE }, $EXTENSION_CLASS_DESCRIPTOR->setVideoInformation(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V
                return-void
                """.toInstructions(),
            null,
            null,
        ),
    ).toMutable()

private fun MutableClass.getModernVideoInformationDispatcherMethod(): MutableMethod =
    ImmutableMethod(
        type,
        "patch_dispatchVideoInformation",
        emptyList(),
        "V",
        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value,
        null,
        null,
        ImmutableMethodImplementation(
            1,
            """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->lastVideoIdIsShort()Z
                move-result v0
                if-eqz v0, :regular
                invoke-static {}, $type->patch_setShortsVideoInformation()V
                :regular
                invoke-static {}, $type->patch_setVideoInformation()V
                return-void
                """.toInstructions(),
            null,
            null,
        ),
    ).toMutable()

private fun MutableMethod.insert(insertIndex: Int, register: String, descriptor: String) =
    addInstruction(insertIndex, "invoke-static/range { $register }, $descriptor")

/**
 * Hook the player controller.  Called when a video is opened or the current video is changed.
 *
 * Note: This hook is called very early and is called before the video id, video time, video length,
 * and many other data fields are set.
 *
 * @param targetMethodClass The descriptor for the class to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
internal fun onCreateHook(targetMethodClass: String, targetMethodName: String) =
    playerConstructorMethod.addInstruction(
        playerConstructorInsertIndex++,
        "invoke-static { }, $targetMethodClass->$targetMethodName()V"
    )

/**
 * Hook the MDX player director. Called when playing videos while casting to a big screen device.
 *
 * @param targetMethodClass The descriptor for the class to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
internal fun onCreateHookMdx(targetMethodClass: String, targetMethodName: String) =
    mdxConstructorMethod.addInstruction(
        mdxConstructorInsertIndex++,
        "invoke-static { }, $targetMethodClass->$targetMethodName()V"
    )

/**
 * Hook the video time.
 * The hook is usually called once per second.
 *
 * @param targetMethodClass The descriptor for the static method to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
internal fun videoTimeHook(targetMethodClass: String, targetMethodName: String) =
    videoTimeConstructorMethod.addInstruction(
        videoTimeConstructorInsertIndex++,
        "invoke-static { p1, p2 }, $targetMethodClass->$targetMethodName(J)V"
    )

/**
 * This method is invoked on both regular videos and Shorts.
 */
internal fun hookVideoInformation(descriptor: String) =
    videoInformationMethod.apply {
        val index = implementation!!.instructions.lastIndex

        insert(
            index,
            "v$REGISTER_CHANNEL_ID .. v$REGISTER_VIDEO_IS_LIVE",
            descriptor
        )
    }

/**
 * This method is invoked only in regular videos.
 */
internal fun hookBackgroundPlayVideoInformation(descriptor: String) =
    backgroundVideoInformationMethod.apply {
        val index = implementation!!.instructions.lastIndex

        insert(
            index,
            "v$REGISTER_CHANNEL_ID .. v$REGISTER_VIDEO_IS_LIVE",
            descriptor
        )
    }

/**
 * This method is invoked only in shorts videos.
 */
internal fun hookShortsVideoInformation(descriptor: String) =
    shortsVideoInformationMethod.apply {
        val index = implementation!!.instructions.lastIndex

        insert(
            index,
            "v$REGISTER_CHANNEL_ID .. v$REGISTER_VIDEO_IS_LIVE",
            descriptor
        )
    }
