package app.revanced.patches.music.utils.fix.client

import app.revanced.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val createPlayerRequestBodyFingerprint = legacyFingerprint(
    name = "createPlayerRequestBodyFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.IGET,
        Opcode.AND_INT_LIT16,
    ),
    strings = listOf("ms"),
)

internal val createPlayerRequestBodyWithVersionReleaseFingerprint = legacyFingerprint(
    name = "createPlayerRequestBodyWithVersionReleaseFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("Google Inc."),
    customFingerprint = { method, _ ->
        indexOfBuildInstruction(method) >= 0
    },
)

fun indexOfBuildInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.name == "build" &&
                reference.parameterTypes.isEmpty() &&
                reference.returnType.startsWith("L")
    }

internal val setPlayerRequestClientTypeFingerprint = legacyFingerprint(
    name = "setPlayerRequestClientTypeFingerprint",
    opcodes = listOf(
        Opcode.IGET,
        Opcode.IPUT, // Sets ClientInfo.clientId.
    ),
    strings = listOf("10.29"),
)

/**
 * This is the fingerprint used in the 'client-spoof' patch around 2022.
 * (Integrated into [baseSpoofUserAgentPatch] now.)
 *
 * This method is modified by [baseSpoofUserAgentPatch], so the fingerprint does not check the [Opcode].
 */
internal val userAgentHeaderBuilderFingerprint = legacyFingerprint(
    name = "userAgentHeaderBuilderFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("(Linux; U; Android "),
)