/*
 * Copyright (C) 2026 anddea
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

package app.morphe.patches.music.video.information

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.toInstructions
import app.morphe.patches.music.utils.extension.Constants.SHARED_PATH
import app.morphe.patches.music.utils.playbackSpeedFingerprint
import app.morphe.patches.music.utils.playbackSpeedParentFingerprint
import app.morphe.patches.music.utils.playservice.is_8_51_or_greater
import app.morphe.patches.music.video.playerresponse.Hook
import app.morphe.patches.music.video.playerresponse.addPlayerResponseMethodHook
import app.morphe.patches.music.video.playerresponse.playerResponseMethodHookPatch
import app.morphe.patches.shared.mdxPlayerDirectorSetVideoStageFingerprint
import app.morphe.patches.shared.videoLengthFingerprintLegacy
import app.morphe.util.addStaticFieldToExtension
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.getReference
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/VideoInformation;"

private const val REGISTER_PLAYER_RESPONSE_MODEL = 4

private const val REGISTER_VIDEO_ID = 0
private const val REGISTER_VIDEO_LENGTH = 1

@Suppress("unused")
private const val REGISTER_VIDEO_LENGTH_DUMMY = 2

private lateinit var PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR: String
private lateinit var videoIdMethodCall: String
private lateinit var videoLengthMethodCall: String

private lateinit var videoInformationMethod: MutableMethod

/**
 * Used in [videoEndFingerprint] and [mdxPlayerDirectorSetVideoStageFingerprint].
 * Since both classes are inherited from the same class,
 * [videoEndFingerprint] and [mdxPlayerDirectorSetVideoStageFingerprint] always have the same [seekSourceEnumType] and [seekSourceMethodName].
 */
private var seekSourceEnumType = ""
private var seekSourceMethodName = ""

private lateinit var playerConstructorMethod: MutableMethod
private var playerConstructorInsertIndex = -1

private lateinit var mdxConstructorMethod: MutableMethod
private var mdxConstructorInsertIndex = -1

private lateinit var videoTimeConstructorMethod: MutableMethod
private var videoTimeConstructorInsertIndex = 2
private var videoTimeRegisters = "p1 .. p2"

val videoInformationPatch = bytecodePatch(
    description = "videoInformationPatch",
) {
    dependsOn(playerResponseMethodHookPatch)

    execute {
        if (is_8_51_or_greater) {
            val playerClass = ModernVideoEndFingerprint.classDef
            val playerType = playerClass.type

            seekSourceEnumType = ModernVideoEndFingerprint.method.parameterTypes[1].toString()
            seekSourceMethodName = ModernVideoEndFingerprint.method.name

            playerClass.methods.add(
                ImmutableMethod(
                    playerType,
                    "seekTo",
                    listOf(ImmutableMethodParameter("J", emptySet(), "time")),
                    "Z",
                    AccessFlags.PUBLIC or AccessFlags.FINAL,
                    emptySet(),
                    null,
                    ImmutableMethodImplementation(
                        4,
                        """
                            sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                            invoke-virtual {p0, p1, p2, v0}, $playerType->$seekSourceMethodName(J$seekSourceEnumType)Z
                            move-result p1
                            return p1
                        """.toInstructions(),
                        null,
                        null
                    )
                ).toMutable()
            )

            addStaticFieldToExtension(
                EXTENSION_CLASS_DESCRIPTOR,
                "overrideVideoTime",
                "videoInformationClass",
                playerType,
                """
                    if-eqz v0, :ignore
                    invoke-virtual {v0, p0, p1}, $playerType->seekTo(J)Z
                    move-result p0
                    return p0
                    :ignore
                    const/4 v0, 0x0
                    return v0
                """
            )

            playerConstructorMethod = playerClass.methods.first { it.name == "<init>" }
            playerConstructorInsertIndex = playerConstructorMethod.indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_DIRECT &&
                        getReference<MethodReference>()?.name == "<init>"
            } + 1
            playerConstructorMethod.addInstruction(
                playerConstructorInsertIndex++,
                "sput-object p0, $EXTENSION_CLASS_DESCRIPTOR->videoInformationClass:$playerType"
            )
            onCreateHook(EXTENSION_CLASS_DESCRIPTOR, "initialize")

            videoTimeConstructorMethod = ModernPlayerControllerSetTimeReferenceFingerprint.method
            videoTimeConstructorInsertIndex = 0
            var parameterRegister = 1
            var longParameterCount = 0
            for (type in videoTimeConstructorMethod.parameterTypes) {
                val parameterType = type.toString()
                if (parameterType == "J") {
                    longParameterCount++
                    if (longParameterCount == 2) {
                        videoTimeRegisters = "p$parameterRegister .. p${parameterRegister + 1}"
                        break
                    }
                }
                parameterRegister += if (parameterType == "J" || parameterType == "D") 2 else 1
            }
            videoTimeHook(EXTENSION_CLASS_DESCRIPTOR, "setVideoTime")

            ModernVideoIdFingerprint.let { fingerprint ->
                fingerprint.method.apply {
                    val playerResponseModelIndex = indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        (opcode == Opcode.INVOKE_INTERFACE_RANGE || opcode == Opcode.INVOKE_INTERFACE) &&
                                reference?.returnType == "Ljava/lang/String;" &&
                                reference.parameterTypes.isEmpty()
                    }

                    PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR =
                        getInstruction<ReferenceInstruction>(playerResponseModelIndex)
                            .getReference<MethodReference>()
                            ?.definingClass
                            ?: throw PatchException("Could not find Player Response Model class")
                    videoIdMethodCall =
                        "invoke-interface {v$REGISTER_PLAYER_RESPONSE_MODEL}, " +
                                getInstruction<ReferenceInstruction>(playerResponseModelIndex).reference

                    val videoLengthMethod = classDefBy(PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR).methods
                        .first { it.returnType == "J" && it.parameterTypes.isEmpty() }
                    videoLengthMethodCall =
                        "invoke-interface {v$REGISTER_PLAYER_RESPONSE_MODEL}, " +
                                "$PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR->${videoLengthMethod.name}()J"

                    videoInformationMethod = getVideoInformationMethod()
                    fingerprint.classDef.methods.add(videoInformationMethod)
                    videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")
                    videoLengthHook("$EXTENSION_CLASS_DESCRIPTOR->setVideoLength(J)V")

                    addInstruction(
                        playerResponseModelIndex + 2,
                        "invoke-direct/range {p0 .. p1}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                    )
                }
            }

            playbackSpeedFingerprint.matchOrThrow(playbackSpeedParentFingerprint).let {
                it.getWalkerMethod(it.instructionMatches.last().index).apply {
                    addInstruction(
                        implementation!!.instructions.lastIndex,
                        "invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->setPlaybackSpeed(F)V"
                    )
                }
            }
            return@execute
        }

        fun addSeekInterfaceMethods(
            targetClass: MutableClass,
            targetMethod: MutableMethod,
            seekMethodName: String,
            methodName: String,
            fieldName: String
        ) {
            targetMethod.apply {
                targetClass.methods.add(
                    ImmutableMethod(
                        definingClass,
                        "seekTo",
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
                        invoke-virtual {v0, p0, p1}, $definingClass->seekTo(J)Z
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

        fun Pair<String, Fingerprint>.getPlayerResponseInstruction(returnType: String): String {
            methodOrThrow().apply {
                val targetReference = getInstruction<ReferenceInstruction>(
                    indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        (opcode == Opcode.INVOKE_INTERFACE_RANGE || opcode == Opcode.INVOKE_INTERFACE) &&
                                reference?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR &&
                                reference.returnType == returnType
                    }
                ).reference

                return "invoke-interface {v$REGISTER_PLAYER_RESPONSE_MODEL}, $targetReference"
            }
        }

        videoEndFingerprint.methodOrThrow().apply {
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

            // Create extension interface methods.
            addSeekInterfaceMethods(
                videoEndFingerprint.mutableClassOrThrow(),
                this,
                seekSourceMethodName,
                "overrideVideoTime",
                "videoInformationClass"
            )
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

            // Create extension interface methods.
            addSeekInterfaceMethods(
                mdxPlayerDirectorSetVideoStageFingerprint.mutableClassOrThrow(),
                this,
                seekSourceMethodName,
                "overrideMDXVideoTime",
                "videoInformationMDXClass"
            )
        }

        /**
         * Set current video information
         */
        videoIdFingerprint.matchOrThrow().let {
            it.method.apply {
                val playerResponseModelIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    (opcode == Opcode.INVOKE_INTERFACE_RANGE || opcode == Opcode.INVOKE_INTERFACE) &&
                            reference?.returnType == "Ljava/lang/String;" &&
                            reference.parameterTypes.isEmpty()
                }

                PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR =
                    getInstruction(playerResponseModelIndex)
                        .getReference<MethodReference>()
                        ?.definingClass
                        ?: throw PatchException("Could not find Player Response Model class")

                videoIdMethodCall =
                    videoIdFingerprint.getPlayerResponseInstruction("Ljava/lang/String;")
                videoLengthMethodCall =
                    videoLengthFingerprintLegacy.getPlayerResponseInstruction("J")

                videoInformationMethod = getVideoInformationMethod()
                it.classDef.methods.add(videoInformationMethod)

                addInstruction(
                    playerResponseModelIndex + 2,
                    "invoke-direct/range {p0 .. p1}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                )
            }
        }

        /**
         * Set the video time method
         */
        playerControllerSetTimeReferenceFingerprint.matchOrThrow().let {
            videoTimeConstructorMethod =
                it.getWalkerMethod(it.instructionMatches.first().index)
        }

        /**
         * Set current video time
         */
        videoTimeHook(EXTENSION_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Set current video length
         */
        videoLengthHook("$EXTENSION_CLASS_DESCRIPTOR->setVideoLength(J)V")

        /**
         * Set current video id
         */
        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")
        addPlayerResponseMethodHook(
            Hook.VideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->setPlayerResponseVideoId(Ljava/lang/String;)V"
            ),
        )
        // Call before any other video id hooks,
        // so they can use VideoInformation and check if the video id is for a Short.
        addPlayerResponseMethodHook(
            Hook.ProtoBufferParameterBeforeVideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
            )
        )
        /**
         * Hook current playback speed
         */
        playbackSpeedFingerprint.matchOrThrow(playbackSpeedParentFingerprint).let {
            it.getWalkerMethod(it.instructionMatches.last().index).apply {
                addInstruction(
                    implementation!!.instructions.lastIndex,
                    "invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->setPlaybackSpeed(F)V"
                )
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
                $videoIdMethodCall
                move-result-object v$REGISTER_VIDEO_ID
                $videoLengthMethodCall
                move-result-wide v$REGISTER_VIDEO_LENGTH
                return-void
                """.toInstructions(),
            null,
            null
        )
    ).toMutable()

private fun MutableMethod.insert(insertIndex: Int, register: String, descriptor: String) =
    addInstruction(insertIndex, "invoke-static { $register }, $descriptor")

private fun MutableMethod.insertTimeHook(insertIndex: Int, descriptor: String) =
    insert(insertIndex, "p1, p2", descriptor)

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

internal fun videoIdHook(
    descriptor: String
) = videoInformationMethod.apply {
    addInstruction(
        implementation!!.instructions.lastIndex,
        "invoke-static {v$REGISTER_VIDEO_ID}, $descriptor"
    )
}

internal fun videoLengthHook(
    descriptor: String
) = videoInformationMethod.apply {
    addInstruction(
        implementation!!.instructions.lastIndex,
        "invoke-static {v$REGISTER_VIDEO_LENGTH, v$REGISTER_VIDEO_LENGTH_DUMMY}, $descriptor"
    )
}

/**
 * Hook the video time.
 * The hook is usually called once per second.
 *
 * @param targetMethodClass The descriptor for the static method to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
internal fun videoTimeHook(targetMethodClass: String, targetMethodName: String) =
    if (is_8_51_or_greater) {
        videoTimeConstructorMethod.addInstruction(
            videoTimeConstructorInsertIndex++,
            "invoke-static/range { $videoTimeRegisters }, $targetMethodClass->$targetMethodName(J)V"
        )
    } else {
        videoTimeConstructorMethod.insertTimeHook(
            videoTimeConstructorInsertIndex++,
            "$targetMethodClass->$targetMethodName(J)V"
        )
    }
