package app.revanced.patches.shared.gms

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
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

internal val googlePlayUtilityFingerprint = legacyFingerprint(
    name = "castContextFetchFingerprint",
    returnType = "I",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L", "I"),
    strings = listOf(
        "This should never happen.",
        "MetadataValueReader"
    )
)

internal val serviceCheckFingerprint = legacyFingerprint(
    name = "serviceCheckFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L", "I"),
    strings = listOf("Google Play Services not available")
)

internal val primeMethodFingerprint = legacyFingerprint(
    name = "primeMethodFingerprint",
    strings = listOf("com.google.android.GoogleCamera", "com.android.vending")
)

internal val certificateFingerprint = legacyFingerprint(
    name = "certificateFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("X.509", "user", "S"),
    customFingerprint = { method, _ ->
        indexOfGetPackageNameInstruction(method) >= 0
    }
)

fun indexOfGetPackageNameInstruction(method: Method) =
    method.indexOfFirstInstruction {
        getReference<MethodReference>()?.toString() == "Landroid/content/Context;->getPackageName()Ljava/lang/String;"
    }