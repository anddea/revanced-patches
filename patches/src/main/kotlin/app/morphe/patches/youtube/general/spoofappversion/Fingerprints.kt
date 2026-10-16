/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.general.spoofappversion

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal object ShortsBoldIconsPrimaryFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        literal(45632000L),
    ),
)

internal object ShortsBoldIconsSecondaryFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        literal(45632045L),
    ),
)

internal object AuthenticationChangeListenerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    custom = { method, _ ->
        indexOfMessageLiteBuilderReference(method) >= 0 &&
                (hasAuthenticationChangedString(method) ||
                        isAuthenticationChangeListenerWithoutString(method))
    },
)

// modified by lavinhoque33, 2026-10-04: string-less detection used by YouTube 21.39+.
private fun isAuthenticationChangeListenerWithoutString(method: Method) =
    method.parameterTypes.size == 1 &&
            method.indexOfFirstInstruction {
                opcode == Opcode.CHECK_CAST &&
                        getReference<TypeReference>()?.type ==
                        "Lcom/google/protos/youtube/api/innertube/InnertubeContext\$InnerTubeContext;"
            } >= 0 &&
            method.indexOfFirstInstruction {
                getReference<MethodReference>()?.let {
                    it.definingClass == "Ljava/util/Set;" && it.name == "iterator"
                } == true
            } >= 0 &&
            indexOfMessageLiteBuilderReference(method) >= 0

private fun hasAuthenticationChangedString(method: Method) =
    method.indexOfFirstInstruction {
        getReference<StringReference>()?.string == "Authentication changed while request was being made"
    } >= 0


internal fun indexOfMessageLiteBuilderReference(method: Method, type: String = "L") =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.parameterTypes?.isEmpty() == true &&
                reference.returnType.startsWith(type)
    }

internal object BuildDummyClientContextBodyFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, name = "instance"),
        string("10.29", location = MatchAfterWithin(10)),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = CLIENT_INFO_CLASS_DESCRIPTOR,
            type = "Ljava/lang/String;",
            location = MatchAfterImmediately(),
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = CLIENT_INFO_CLASS_DESCRIPTOR,
        ),
    ),
)

internal object ReelCreateItemsEndpointConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("reel/create_reel_items"),
)

internal object ReelItemWatchEndpointConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("reel/reel_item_watch"),
)

internal object ReelWatchSequenceEndpointConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("reel/reel_watch_sequence"),
)
