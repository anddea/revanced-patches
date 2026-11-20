/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.notifications

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.newInstance
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Header map helper of the network response class, which holds the raw response body.
 */
private object NetworkResponseHeadersFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "Ljava/util/Map;",
    parameters = listOf("Ljava/util/List;"),
    filters = listOf(
        newInstance("Ljava/util/TreeMap;"),
        fieldAccess(
            opcode = Opcode.SGET_OBJECT,
            definingClass = "Ljava/lang/String;",
            name = "CASE_INSENSITIVE_ORDER",
        )
    )
)

/**
 * Constructor with the body as one byte array.
 */
internal object NetworkResponseBytesConstructorFingerprint : Fingerprint(
    classFingerprint = NetworkResponseHeadersFingerprint,
    name = "<init>",
    parameters = listOf("I", "[B", "Ljava/util/Map;", "Ljava/util/List;", "Z"),
)

/**
 * Constructor with the body as byte buffers. YouTube 21.40+.
 */
internal object NetworkResponseBuffersConstructorFingerprint : Fingerprint(
    classFingerprint = NetworkResponseHeadersFingerprint,
    name = "<init>",
    parameters = listOf("I", "Ljava/lang/Iterable;", "Ljava/util/List;", "Z", "Z"),
)

/**
 * Constructor with the body as byte buffers. YouTube 21.39 and lower.
 */
internal object NetworkResponseBuffersConstructorLegacyFingerprint : Fingerprint(
    classFingerprint = NetworkResponseHeadersFingerprint,
    name = "<init>",
    parameters = listOf("I", "Ljava/lang/Iterable;", "Ljava/util/List;"),
)

/**
 * Older responses store only a byte array and build the header map in a constructor.
 * Hook the shared four-argument constructor so delegating overloads transform the body once.
 */
internal object NetworkResponseBytesConstructorOldFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        name = "<init>",
        parameters = listOf("I", "[B", "Ljava/util/List;"),
        filters = listOf(
            newInstance("Ljava/util/TreeMap;"),
            fieldAccess(
                opcode = Opcode.SGET_OBJECT,
                definingClass = "Ljava/lang/String;",
                name = "CASE_INSENSITIVE_ORDER",
            )
        )
    ),
    name = "<init>",
    parameters = listOf("I", "[B", "Ljava/util/Map;", "Ljava/util/List;"),
)

/** YouTube 19.28 also carries cache status and network timing in its response. */
internal object NetworkResponseBytesConstructorV1928Fingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        name = "<init>",
        parameters = listOf("I", "[B", "Z", "J", "Ljava/util/List;"),
        filters = listOf(
            newInstance("Ljava/util/TreeMap;"),
            fieldAccess(
                opcode = Opcode.SGET_OBJECT,
                definingClass = "Ljava/lang/String;",
                name = "CASE_INSENSITIVE_ORDER",
            )
        )
    ),
    name = "<init>",
    parameters = listOf("I", "[B", "Ljava/util/Map;", "Ljava/util/List;", "Z", "J"),
)
