package app.morphe.patches.youtube.general.transcript

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val transcriptUrlFingerprint = legacyFingerprint(
    name = "transcriptUrlFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { method, _ ->
        indexOfTranscriptUrlRequestBuilderInstruction(method) >= 0 &&
                indexOfNewTranscriptUrlRequestBuilderInstruction(method) >= 0 &&
                indexOfUploadDataProvidersInstruction(method) >= 0
    }
)

internal fun indexOfTranscriptUrlRequestBuilderInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>().toString() == "Lorg/chromium/net/UrlRequest\$Builder;->build()Lorg/chromium/net/UrlRequest;"
    }

internal fun indexOfNewTranscriptUrlRequestBuilderInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>().toString() == "Lorg/chromium/net/CronetEngine;->newUrlRequestBuilder(Ljava/lang/String;Lorg/chromium/net/UrlRequest\$Callback;Ljava/util/concurrent/Executor;)Lorg/chromium/net/UrlRequest\$Builder;"
    }

internal fun indexOfUploadDataProvidersInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_STATIC &&
                getReference<MethodReference>().toString() == "Lorg/chromium/net/UploadDataProviders;->create(Ljava/nio/ByteBuffer;)Lorg/chromium/net/UploadDataProvider;"
    }
