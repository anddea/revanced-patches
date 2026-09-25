/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.utils.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.youtube.utils.resourceid.icOfflineNoContentUpsideDown
import app.morphe.patches.youtube.utils.resourceid.offlineNoContentBodyTextNotOfflineEligible
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SpecificNetworkErrorViewControllerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        literal(icOfflineNoContentUpsideDown),
        literal(offlineNoContentBodyTextNotOfflineEligible),
        methodCall(name = "getString", returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
    )
)

// It's not clear if this second class is ever used and it may be dead code,
// but it the layout image / text is identical to the network error fingerprint above.
internal object LoadingFrameLayoutControllerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        literal(icOfflineNoContentUpsideDown),
        literal(offlineNoContentBodyTextNotOfflineEligible),
        methodCall(name = "getString", returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
    )
)

internal object GmsDeviceComplianceCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"), // Rx single emitter.
    filters = listOf(
        string("failsafe_enable_gms_device_compliance_check"),
        // GServices lookup of the flag above.
        methodCall(parameters = listOf("Ljava/lang/String;"), returnType = "Z"),
        // Emits false and returns when the check is turned off.
        methodCall(
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "V",
            opcode = Opcode.INVOKE_VIRTUAL
        )
    )
)
