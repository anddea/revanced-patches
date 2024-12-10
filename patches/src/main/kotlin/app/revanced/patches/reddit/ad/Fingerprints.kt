package app.revanced.patches.reddit.ad

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val commentAdsFingerprint = legacyFingerprint(
    name = "commentAdsFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/PostDetailPresenter\$loadAd\$1;") &&
                method.name == "invokeSuspend"
    },
)

internal val adPostFingerprint = legacyFingerprint(
    name = "adPostFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT
    ),
    // "children" are present throughout multiple versions
    strings = listOf(
        "children",
        "uxExperiences"
    ),
    customFingerprint = { method, classDef ->
        method.definingClass.endsWith("/Listing;") &&
                method.name == "<init>" &&
                classDef.sourceFile == "Listing.kt"
    },
)

internal val newAdPostFingerprint = legacyFingerprint(
    name = "newAdPostFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(Opcode.INVOKE_VIRTUAL),
    strings = listOf(
        "chain",
        "feedElement"
    ),
    customFingerprint = { _, classDef -> classDef.sourceFile == "AdElementConverter.kt" },
)
