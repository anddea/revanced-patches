package app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.BuildRequestFingerprint.indexOfBuildUrlRequestInstruction
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.BuildRequestFingerprint.indexOfEntrySetInstruction
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.BuildRequestFingerprint.indexOfNewUrlRequestBuilderInstruction
import app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints.BuildRequestFingerprint.indexOfRequestFinishedListenerInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object BuildRequestFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ ->
        methodDef.implementation != null &&
                indexOfRequestFinishedListenerInstruction(methodDef) >= 0 &&
                !methodDef.definingClass.startsWith("Lorg/") &&
                indexOfNewUrlRequestBuilderInstruction(methodDef) >= 0 &&
                indexOfBuildUrlRequestInstruction(methodDef) >= 0 &&
                // YouTube 17.34.36 ~ YouTube 18.35.36
                (indexOfEntrySetInstruction(methodDef) >= 0 ||
                        // YouTube 18.36.39 ~
                        methodDef.parameters[1].type == "Ljava/util/Map;")
    }
) {
    fun indexOfRequestFinishedListenerInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>().toString() == "Lorg/chromium/net/ExperimentalUrlRequest${'$'}Builder;->setRequestFinishedListener(Lorg/chromium/net/RequestFinishedInfo${'$'}Listener;)Lorg/chromium/net/ExperimentalUrlRequest${'$'}Builder;"
        }

    fun indexOfNewUrlRequestBuilderInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>().toString() == "Lorg/chromium/net/CronetEngine;->newUrlRequestBuilder(Ljava/lang/String;Lorg/chromium/net/UrlRequest${'$'}Callback;Ljava/util/concurrent/Executor;)Lorg/chromium/net/UrlRequest${'$'}Builder;"
        }

    fun indexOfBuildUrlRequestInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>().toString() == "Lorg/chromium/net/ExperimentalUrlRequest${'$'}Builder;->build()Lorg/chromium/net/ExperimentalUrlRequest;"
        }

    fun indexOfEntrySetInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_INTERFACE &&
                    getReference<MethodReference>().toString() == "Ljava/util/Map;->entrySet()Ljava/util/Set;"
        }
}
