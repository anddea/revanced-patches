package app.morphe.patches.shared.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.opcode
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.MethodUtil

const val GET_GMS_CORE_VENDOR_GROUP_ID_METHOD_NAME = "getGmsCoreVendorGroupId"

internal val gmsCoreSupportFingerprint = Fingerprint(
    custom = { _, classDef ->
        classDef.endsWith("GmsCoreSupport;")
    }
)

internal val castContextFetchFingerprint = Fingerprint(
    strings = listOf("Error fetching CastContext.")
)

internal val droidGuardSignatureFingerprint = Fingerprint(
    returnType = "Z",
    strings = listOf(
        "SHA-256",
        "Failed to verify signatures"
    )
)

internal val gmsServiceBrokerFingerprint = Fingerprint(
    returnType = "V",
    strings = listOf("mServiceBroker is null, client disconnected")
)

internal val googlePlayUtilityFingerprint = Fingerprint(
    returnType = "I",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L", "I"),
    strings = listOf(
        "This should never happen.",
        "MetadataValueReader",
    )
)

internal val serviceCheckFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L", "I"),
    strings = listOf("Google Play Services not available")
)

internal val sslGuardFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    strings = listOf("Cannot initialize SslGuardSocketFactory will null"),
)

internal val eCatcherFingerprint = Fingerprint(
    returnType = "V",
    filters = listOf(opcode(Opcode.NEW_ARRAY)),
    strings = listOf("ECatcher disabled: level: %s, category: %s, message: %s"),
    custom = { method, _ ->
        method.parameterTypes.contains("Ljava/util/function/Function;")
    },
)

internal val primesApiFingerprint = Fingerprint(
    returnType = "V",
    strings = listOf("PrimesApiImpl.java"),
    custom = { method, _ ->
        MethodUtil.isConstructor(method)
    }
)

internal val primesBackgroundInitializationFingerprint = Fingerprint(
    filters = listOf(opcode(Opcode.NEW_INSTANCE)),
    custom = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.CONST_STRING &&
                    getReference<StringReference>()
                        ?.string.toString()
                        .startsWith("Primes init triggered from background in package:")
        } >= 0
    }
)

internal val primesLifecycleEventFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(opcode(Opcode.NEW_INSTANCE)),
    custom = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.CONST_STRING &&
                    getReference<StringReference>()
                        ?.string.toString()
                        .startsWith("Primes did not observe lifecycle events in the expected order.")
        } >= 0
    }
)

internal val primeMethodFingerprint = Fingerprint(
    strings = listOf("com.google.android.GoogleCamera", "com.android.vending")
)
