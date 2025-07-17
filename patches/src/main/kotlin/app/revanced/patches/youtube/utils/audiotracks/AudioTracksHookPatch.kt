package app.revanced.patches.youtube.utils.audiotracks

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.util.fingerprint.originalMethodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private lateinit var isDefaultMethod: MutableMethod
private lateinit var audioTrackIdMethod: MutableMethod

private const val helperMethodName = "extension_isDefaultAudioTrack"

val audioTracksHookPatch = bytecodePatch(
    description = "audioTracksHookPatch"
) {
    dependsOn(sharedExtensionPatch)

    execute {

        fun Method.firstFormatStreamingModelCall(
            returnType: String = "Ljava/lang/String;"
        ): MutableMethod {
            val audioTrackIdIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<MethodReference>()
                reference?.definingClass == "Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;"
                        && reference.returnType == returnType
            }

            return navigate(this).to(audioTrackIdIndex).stop()
        }

        // Accessor methods of FormatStreamModel have no string constants and
        // opcodes are identical to other methods in the same class,
        // so must walk from another class that use the methods.
        isDefaultMethod = streamingModelBuilderFingerprint.originalMethodOrThrow()
            .firstFormatStreamingModelCall("Z")
        audioTrackIdMethod =
            menuItemAudioTrackFingerprint.originalMethodOrThrow().firstFormatStreamingModelCall()
        val audioTrackDisplayNameMethod =
            audioStreamingTypeSelector.originalMethodOrThrow().firstFormatStreamingModelCall()
        val formatStreamModelClass = proxy(classes.first {
            it.type == audioTrackIdMethod.definingClass
        }).mutableClass

        formatStreamModelClass.apply {
            // Add a new field to store the override.
            val helperFieldName = "isDefaultAudioTrackOverride"
            fields.add(
                ImmutableField(
                    type,
                    helperFieldName,
                    "Ljava/lang/Boolean;",
                    // Boolean is a 100% immutable class (all fields are final)
                    // and safe to write to a shared field without volatile/synchronization,
                    // but without volatile the field can show stale data
                    // and the same field is calculated more than once by different threads.
                    AccessFlags.PRIVATE.value or AccessFlags.VOLATILE.value,
                    null,
                    annotations,
                    null
                ).toMutable()
            )

            // Add a helper method because the isDefaultAudioTrack() has only 2 registers and 3 are needed.
            val helperMethodClass = type
            val helperMethod = ImmutableMethod(
                helperMethodClass,
                helperMethodName,
                listOf(ImmutableMethodParameter("Z", annotations, null)),
                "Z",
                AccessFlags.PRIVATE.value,
                annotations,
                null,
                MutableMethodImplementation(6),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0,
                    """
                        iget-object v0, p0, $helperMethodClass->$helperFieldName:Ljava/lang/Boolean;
                        if-eqz v0, :call_extension            
                        invoke-virtual { v0 }, Ljava/lang/Boolean;->booleanValue()Z
                        move-result v3
                        return v3
                        
                        :call_extension
                        invoke-virtual { p0 }, $audioTrackIdMethod
                        move-result-object v1
                        
                        invoke-virtual { p0 }, $audioTrackDisplayNameMethod
                        move-result-object v2
    
                        invoke-static { p1, v1, v2 }, $GENERAL_CLASS_DESCRIPTOR->isDefaultAudioStream(ZLjava/lang/String;Ljava/lang/String;)Z
                        move-result v3
                        
                        invoke-static { v3 }, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                        move-result-object v0
                        iput-object v0, p0, $helperMethodClass->$helperFieldName:Ljava/lang/Boolean;
                        return v3
                    """
                )
            }
            methods.add(helperMethod)
        }
    }
}

// Modify isDefaultAudioTrack() to call extension helper method.
internal fun disableForcedAudioTracks() =
    isDefaultMethod.apply {
        val index = indexOfFirstInstructionOrThrow(Opcode.RETURN)
        val register = getInstruction<OneRegisterInstruction>(index).registerA

        addInstructions(
            index,
            """
                invoke-direct { p0, v$register }, $definingClass->$helperMethodName(Z)Z
                move-result v$register
                """
        )
    }

internal fun hookAudioTrackId(descriptor: String) =
    isDefaultMethod.apply {
        addInstructions(
            0, """
                invoke-virtual { p0 }, $audioTrackIdMethod
                move-result-object v0
                invoke-static {v0}, $descriptor
                """
        )
    }
