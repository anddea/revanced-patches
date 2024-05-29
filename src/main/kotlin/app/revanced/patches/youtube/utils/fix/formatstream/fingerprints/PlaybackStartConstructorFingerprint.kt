package app.revanced.patches.youtube.utils.fix.formatstream.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PlaybackStartConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IPUT,
        Opcode.IGET_OBJECT, // type: Lcom/google/protos/youtube/api/innertube/StreamingDataOuterClass$StreamingData;
        Opcode.IF_NEZ
    ),
)

