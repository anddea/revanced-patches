package app.morphe.patches.shared.customspeed

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.literal
import app.morphe.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ArrayGeneratorFingerprint : Fingerprint(
    returnType = "[L",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_4,
        Opcode.NEW_ARRAY,
    ),
    strings = listOf("0.0#")
)

internal object LimiterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("F", "L"),
    strings = listOf("setPlaybackRate"),
    custom = { method, _ ->
        method.containsLiteralInstruction(0.25f.toRawBits().toLong()) &&
                method.containsLiteralInstruction(4.0f.toRawBits().toLong())
    }
)

internal object ServerSideMaxSpeedFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    filters = listOf(
        literal(45719140L)
    )
)

internal object LimiterFallBackFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_HIGH16,
        Opcode.CONST_HIGH16,
        Opcode.INVOKE_STATIC,
    ),
    strings = listOf("Playback rate: %f")
)

internal object LimiterLegacyFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("F"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_HIGH16,
        Opcode.CONST_HIGH16,
        Opcode.INVOKE_STATIC,
    )
)
