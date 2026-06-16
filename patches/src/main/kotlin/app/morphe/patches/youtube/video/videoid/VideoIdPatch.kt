package app.morphe.patches.youtube.video.videoid

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.video.playerresponse.Hook
import app.morphe.patches.youtube.video.playerresponse.addPlayerResponseMethodHook
import app.morphe.patches.youtube.video.playerresponse.playerResponseMethodHookPatch
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.lang.ref.WeakReference

private lateinit var videoIdMethodRef: WeakReference<MutableMethod>
private var videoIdRegister = -1
private var videoIdInsertIndex = -1

private lateinit var backgroundPlaybackMethodRef: WeakReference<MutableMethod>
private var backgroundPlaybackVideoIdRegister = -1
private var backgroundPlaybackInsertIndex = -1

val videoIdPatch = bytecodePatch(
    description = "videoIdPatch",
) {
    dependsOn(
        sharedExtensionPatch,
        playerResponseMethodHookPatch,
    )

    execute {
        videoIdFingerprint.matchOrThrow().let { result ->
            result.method.apply {
                videoIdMethodRef = WeakReference(this)
                val index = indexOfFirstInstructionOrThrow {
                    val methodReference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_INTERFACE &&
                            methodReference != null &&
                            methodReference.returnType == "Ljava/lang/String;" &&
                            methodReference.parameterTypes.isEmpty() &&
                            methodReference.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
                }
                videoIdRegister = getInstruction<OneRegisterInstruction>(index + 1).registerA
                videoIdInsertIndex = index + 2
            }
        }

        videoIdBackgroundPlayFingerprint.let {
            it.method.apply {
                backgroundPlaybackMethodRef = WeakReference(this)
                val index = it.instructionMatches.first().index
                backgroundPlaybackVideoIdRegister = getInstruction<OneRegisterInstruction>(index + 1).registerA
                backgroundPlaybackInsertIndex = index + 2
            }
        }
    }
}

/**
 * Hooks the new video id when the video changes.
 *
 * Supports all videos (regular videos and Shorts).
 *
 * _Does not function if playing in the background with no video visible_.
 *
 * Be aware, this can be called multiple times for the same video id.
 *
 * @param methodDescriptor which method to call. Params have to be `Ljava/lang/String;`
 */
internal fun hookVideoId(
    methodDescriptor: String
) = videoIdMethodRef.get()!!.addInstruction(
    videoIdInsertIndex++, "invoke-static {v$videoIdRegister}, $methodDescriptor"
)

/**
 * Hooks the video id of every video when loaded.
 * Supports all videos and functions in all situations.
 *
 * First parameter is the video id.
 * Second parameter is if the video is a Short AND it is being opened or is currently playing.
 *
 * Hook is always called off the main thread.
 *
 * This hook is called as soon as the player response is parsed,
 * and called before many other hooks are updated such as [playerTypeHookPatch].
 *
 * Note: The video id returned here may not be the current video that's being played.
 * It's common for multiple Shorts to load at once in preparation
 * for the user swiping to the next Short.
 *
 * For most use cases, you probably want to use [hookVideoId] instead.
 *
 * Be aware, this can be called multiple times for the same video id.
 *
 * @param methodDescriptor which method to call. Params must be `Ljava/lang/String;Z`
 */
internal fun hookPlayerResponseVideoId(methodDescriptor: String) = addPlayerResponseMethodHook(
    Hook.VideoId(
        methodDescriptor,
    ),
)
