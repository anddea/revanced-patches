package app.revanced.patches.music.actionbar.components

import app.revanced.patches.music.utils.resourceid.likeDislikeContainer
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val actionBarComponentFingerprint = legacyFingerprint(
    name = "actionBarComponentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.AND_INT_LIT16,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.SGET_OBJECT
    ),
    literals = listOf(99180L),
)

internal val browseSectionListReloadEndpointFingerprint = legacyFingerprint(
    name = "browseSectionListReloadEndpointFingerprint",
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    strings = listOf("request_mutator"),
    customFingerprint = { method, _ ->
        indexOfGetLithoViewFromMapInstruction(method) >= 0
    }
)

internal fun indexOfGetLithoViewFromMapInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_STATIC &&
                reference?.returnType == "Ljava/lang/Object;" &&
                reference.parameterTypes ==
                listOf(
                    "Ljava/util/Map;",
                    "Ljava/lang/Object;",
                    "Ljava/lang/Class;"
                )
    }

internal val likeDislikeContainerFingerprint = legacyFingerprint(
    name = "likeDislikeContainerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(likeDislikeContainer)
)

internal val offlineVideoEndpointFingerprint = legacyFingerprint(
    name = "offlineVideoEndpointFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    strings = listOf("Object is not an offlineable video: %s")
)
