package app.revanced.patches.youtube.general.downloads.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.general.downloads.fingerprints.DownloadPlaylistButtonOnClickFingerprint.indexOfPlaylistDownloadActionInvokeInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object DownloadPlaylistButtonOnClickFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    opcodes = listOf(Opcode.INVOKE_VIRTUAL_RANGE),
    customFingerprint = { methodDef, _ ->
        indexOfPlaylistDownloadActionInvokeInstruction(methodDef) >= 0
    }
) {
    fun indexOfPlaylistDownloadActionInvokeInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.parameterTypes ==
                    listOf(
                        "Ljava/lang/String;",
                        "Lcom/google/android/apps/youtube/app/offline/ui/OfflineArrowView;",
                        "I",
                        "Landroid/view/View${'$'}OnClickListener;"
                    )
        }
}