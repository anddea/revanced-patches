package app.revanced.patches.shared.imageurl

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val onFailureFingerprint = legacyFingerprint(
    name = "onFailureFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf(
        "Lorg/chromium/net/UrlRequest;",
        "Lorg/chromium/net/UrlResponseInfo;",
        "Lorg/chromium/net/CronetException;"
    ),
    customFingerprint = { method, _ ->
        method.name == "onFailed"
    }
)

// Acts as a parent fingerprint.
internal val onResponseStartedFingerprint = legacyFingerprint(
    name = "onResponseStartedFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Lorg/chromium/net/UrlRequest;", "Lorg/chromium/net/UrlResponseInfo;"),
    strings = listOf(
        "Content-Length",
        "Content-Type",
        "identity",
        "application/x-protobuf"
    ),
    customFingerprint = { method, _ ->
        method.name == "onResponseStarted"
    }
)

internal val onSucceededFingerprint = legacyFingerprint(
    name = "onSucceededFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Lorg/chromium/net/UrlRequest;", "Lorg/chromium/net/UrlResponseInfo;"),
    customFingerprint = { method, _ ->
        method.name == "onSucceeded"
    }
)

internal const val CRONET_URL_REQUEST_CLASS_DESCRIPTOR = "Lorg/chromium/net/impl/CronetUrlRequest;"

internal val requestFingerprint = legacyFingerprint(
    name = "requestFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { _, classDef ->
        classDef.type == CRONET_URL_REQUEST_CLASS_DESCRIPTOR
    }
)

internal val messageDigestImageUrlFingerprint = legacyFingerprint(
    name = "messageDigestImageUrlFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("Ljava/lang/String;", "L")
)

internal val messageDigestImageUrlParentFingerprint = legacyFingerprint(
    name = "messageDigestImageUrlParentFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("@#&=*+-_.,:!?()/~'%;\$"),
)
