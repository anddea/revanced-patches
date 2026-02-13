package app.morphe.patches.youtube.video.playbackstart

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference

internal lateinit var playbackStartVideoIdReference: Reference

val playbackStartDescriptorPatch = bytecodePatch(
    description = "playbackStartDescriptorPatch"
) {
    dependsOn(sharedExtensionPatch)

    execute {
        // Find the obfuscated method name for PlaybackStartDescriptor.videoId()
        playbackStartFeatureFlagFingerprint.methodOrThrow().apply {
            val stringMethodIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<MethodReference>()
                reference?.definingClass == PLAYBACK_START_DESCRIPTOR_CLASS_DESCRIPTOR
                        && reference.returnType == "Ljava/lang/String;"
            }

            playbackStartVideoIdReference =
                getInstruction<ReferenceInstruction>(stringMethodIndex).reference
        }
    }
}

