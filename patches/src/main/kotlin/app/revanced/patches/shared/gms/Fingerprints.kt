package app.revanced.patches.shared.gms

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.MethodUtil

const val GET_GMS_CORE_VENDOR_GROUP_ID_METHOD_NAME = "getGmsCoreVendorGroupId"

internal val gmsCoreSupportFingerprint = legacyFingerprint(
    name = "gmsCoreSupportFingerprint",
    customFingerprint = { _, classDef ->
        classDef.endsWith("GmsCoreSupport;")
    }
)

internal val castContextFetchFingerprint = legacyFingerprint(
    name = "castContextFetchFingerprint",
    strings = listOf("Error fetching CastContext.")
)

internal val castDynamiteModuleFingerprint = legacyFingerprint(
    name = "castDynamiteModuleFingerprint",
    strings = listOf("com.google.android.gms.cast.framework.internal.CastDynamiteModuleImpl")
)

internal val castDynamiteModuleV2Fingerprint = legacyFingerprint(
    name = "castDynamiteModuleV2Fingerprint",
    strings = listOf("Failed to load module via V2: ")
)

internal val googlePlayUtilityFingerprint = legacyFingerprint(
    name = "googlePlayUtilityFingerprint",
    returnType = "I",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L", "I"),
    strings = listOf(
        "This should never happen.",
        "MetadataValueReader",
    )
)

internal val serviceCheckFingerprint = legacyFingerprint(
    name = "serviceCheckFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L", "I"),
    strings = listOf("Google Play Services not available")
)

internal val sslGuardFingerprint = legacyFingerprint(
    name = "sslGuardFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("Cannot initialize SslGuardSocketFactory will null"),
)

internal val eCatcherFingerprint = legacyFingerprint(
    name = "eCatcherFingerprint",
    returnType = "V",
    opcodes = listOf(Opcode.NEW_ARRAY),
    strings = listOf("ECatcher disabled: level: %s, category: %s, message: %s"),
    customFingerprint = { method, _ ->
        method.parameterTypes.contains("Ljava/util/function/Function;")
    },
)

internal val primesApiFingerprint = legacyFingerprint(
    name = "primesApiFingerprint",
    returnType = "V",
    strings = listOf("PrimesApiImpl.java"),
    customFingerprint = { method, _ ->
        MethodUtil.isConstructor(method)
    }
)

internal val primesBackgroundInitializationFingerprint = legacyFingerprint(
    name = "primesBackgroundInitializationFingerprint",
    opcodes = listOf(Opcode.NEW_INSTANCE),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.CONST_STRING &&
                    getReference<StringReference>()
                        ?.string.toString()
                        .startsWith("Primes init triggered from background in package:")
        } >= 0
    }
)

internal val primesLifecycleEventFingerprint = legacyFingerprint(
    name = "primesLifecycleEventFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "V",
    parameters = emptyList(),
    opcodes = listOf(Opcode.NEW_INSTANCE),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.CONST_STRING &&
                    getReference<StringReference>()
                        ?.string.toString()
                        .startsWith("Primes did not observe lifecycle events in the expected order.")
        } >= 0
    }
)

internal val primeMethodFingerprint = legacyFingerprint(
    name = "primesLifecycleEventFingerprint",
    strings = listOf("com.google.android.GoogleCamera", "com.android.vending")
)