package app.revanced.patches.reddit.ad

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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
    customFingerprint = { _, classDef ->
        classDef.type.endsWith("/Listing;")
    },
)

internal val newAdPostFingerprint = legacyFingerprint(
    name = "newAdPostFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf(
        "feedElement",
        "android_feed_freeform_render_variant",
    ),
    customFingerprint = { method, _ ->
        indexOfAddArrayListInstruction(method) >= 0
    },
)

internal val newAdPostLegacyFingerprint = legacyFingerprint(
    name = "newAdPostLegacyFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(Opcode.INVOKE_VIRTUAL),
    strings = listOf(
        "chain",
        "feedElement"
    ),
    customFingerprint = { method, classDef ->
        classDef.sourceFile == "AdElementConverter.kt" &&
                indexOfAddArrayListInstruction(method) >= 0
    },
)

internal fun indexOfAddArrayListInstruction(method: Method, index: Int = 0) =
    method.indexOfFirstInstruction(index) {
        getReference<MethodReference>()?.toString() == "Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z"
    }

