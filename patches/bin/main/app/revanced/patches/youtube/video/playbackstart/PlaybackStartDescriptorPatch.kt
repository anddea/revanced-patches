package app.revanced.patches.youtube.video.playbackstart

import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
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

            playbackStartVideoIdReference = getInstruction<ReferenceInstruction>(stringMethodIndex).reference
        }
    }
}

