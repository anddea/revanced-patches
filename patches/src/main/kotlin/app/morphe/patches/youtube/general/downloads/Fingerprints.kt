package app.morphe.patches.youtube.general.downloads

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import app.morphe.util.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private val ENDS_WITH_PARAMETER_LIST = listOf(
    "Lcom/google/android/apps/youtube/app/offline/ui/OfflineArrowView;",
    "I",
    "Landroid/view/View${'$'}OnClickListener;"
)

internal val accessibilityOfflineButtonSyncFingerprint = legacyFingerprint(
    name = "accessibilityOfflineButtonSyncFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    customFingerprint = custom@{ method, _ ->
        if (!MethodUtil.isConstructor(method)) {
            return@custom false
        }
        val parameterTypes = method.parameterTypes
        val parameterSize = parameterTypes.size
        if (parameterSize < 6) {
            return@custom false
        }

        val endsWithMethodParameterList = parameterTypes.slice(parameterSize - 3..<parameterSize)
        parametersEqual(ENDS_WITH_PARAMETER_LIST, endsWithMethodParameterList)
    }
)

internal val downloadPlaylistButtonOnClickFingerprint = legacyFingerprint(
    name = "downloadPlaylistButtonOnClickFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    opcodes = listOf(Opcode.INVOKE_VIRTUAL_RANGE),
    customFingerprint = { method, _ ->
        indexOfPlaylistDownloadActionInvokeInstruction(method) >= 0
    }
)

internal fun indexOfPlaylistDownloadActionInvokeInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.parameterTypes ==
                listOf(
                    "Ljava/lang/String;",
                    "Lcom/google/android/apps/youtube/app/offline/ui/OfflineArrowView;",
                    "I",
                    "Landroid/view/View${'$'}OnClickListener;"
                )
    }

internal val offlinePlaylistEndpointFingerprint = legacyFingerprint(
    name = "offlinePlaylistEndpointFingerprint",
    returnType = "V",
    strings = listOf("Object is not an offlineable playlist: ")
)

internal val offlineVideoEndpointFingerprint = legacyFingerprint(
    name = "offlineVideoEndpointFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf(
        "Ljava/util/Map;",
        "L",
        "Ljava/lang/String", // VideoId
        "L"
    ),
    strings = listOf("Object is not an offlineable video: ")
)

internal val setPlaylistDownloadButtonVisibilityFingerprint = legacyFingerprint(
    name = "setPlaylistDownloadButtonVisibilityFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.IGET,
        Opcode.CONST_4
    )
)