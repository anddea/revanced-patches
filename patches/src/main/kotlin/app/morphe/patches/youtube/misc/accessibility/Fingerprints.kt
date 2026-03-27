package app.morphe.patches.youtube.misc.accessibility

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/**
 * The name of the class to find is 'Lcom/google/android/apps/youtube/app/player/overlay/accessibility/PlayerAccessibilitySettingsEduController$LifecycleObserver;'.
 * It is one of the types of fields in the class.
 * This class name has been obfuscated since YouTube 18.25.40.
 * This class is always the same structure despite not a synthetic class.
 * (Same field and method, checked in YouTube 17.34.36 ~ 20.02.41).
 */
internal val playerAccessibilitySettingsEduControllerParentFingerprint = legacyFingerprint(
    name = "playerAccessibilitySettingsEduControllerParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.STATIC or AccessFlags.CONSTRUCTOR,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.SGET_OBJECT,
        Opcode.CONST_WIDE_16,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.SPUT_WIDE,
        Opcode.RETURN_VOID,
    ),
    customFingerprint = custom@{ method, classDef ->
        // The number of fields is always 12.
        if (classDef.fields.count() != 12) {
            return@custom false
        }
        // The number of methods is always 2 or 3.
        if (classDef.methods.count() > 3) {
            return@custom false
        }
        val implementation = method.implementation
            ?: return@custom false
        val instructions = implementation.instructions
        val instructionCount = instructions.count()
        if (instructionCount != 6) {
            return@custom false
        }

        ((instructions.elementAt(0) as? ReferenceInstruction)?.reference as? FieldReference)?.toString() == "Ljava/util/concurrent/TimeUnit;->DAYS:Ljava/util/concurrent/TimeUnit;"
    }
)
