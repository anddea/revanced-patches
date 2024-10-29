package app.revanced.patches.youtube.utils.fix.suggestedvideoendscreen.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * This fingerprint is also compatible with very old YouTube versions.
 * Tested on YouTube v16.40.36, v18.29.38, v19.16.39.
 */
internal object RemoveOnLayoutChangeListenerFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IPUT,
        Opcode.INVOKE_VIRTUAL
    ),
    // This is the only reference present in the entire smali.
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstruction {
            getReference<MethodReference>()?.toString()
                ?.endsWith("YouTubePlayerOverlaysLayout;->removeOnLayoutChangeListener(Landroid/view/View${'$'}OnLayoutChangeListener;)V") == true
        } >= 0
    }
)