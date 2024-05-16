package app.revanced.patches.youtube.utils.fix.suggestedvideoendscreen.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.ReferenceFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * This fingerprint is also compatible with very old YouTube versions.
 * Tested on YouTube v16.40.36, v18.29.38, v19.12.41.
 */
internal object RemoveOnLayoutChangeListenerFingerprint : ReferenceFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IPUT,
        Opcode.INVOKE_VIRTUAL
    ),
    // This is the only reference present in the entire smali.
    reference = { "YouTubePlayerOverlaysLayout;->removeOnLayoutChangeListener(Landroid/view/View${'$'}OnLayoutChangeListener;)V" }
)