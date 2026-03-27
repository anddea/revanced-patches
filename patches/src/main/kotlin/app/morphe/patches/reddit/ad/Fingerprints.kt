package app.morphe.patches.reddit.ad

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

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

internal val commentAdCommentScreenAdViewFingerprint = legacyFingerprint(
    name = "commentAdCommentScreenAdViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("ad"),
    customFingerprint = { _, classDef ->
        classDef.type.endsWith("/CommentScreenAdView;")
    },
)

internal val commentAdDetailListHeaderViewFingerprint = legacyFingerprint(
    name = "commentAdDetailListHeaderViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("ad"),
    customFingerprint = { _, classDef ->
        classDef.type.endsWith("/DetailListHeaderView;")
    },
)

internal val commentsViewModelFingerprint = legacyFingerprint(
    name = "commentsViewModelFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Z", "L", "I"),
    customFingerprint = { method, classDef ->
        classDef.superclass == "Lcom/reddit/screen/presentation/CompositionViewModel;" &&
                method.indexOfFirstInstruction {
                    opcode == Opcode.NEW_INSTANCE &&
                            getReference<TypeReference>()?.type?.startsWith("Lcom/reddit/postdetail/comment/refactor/CommentsViewModel\$LoadAdsSeparately\$") == true
                } >= 0
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

