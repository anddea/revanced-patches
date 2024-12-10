package app.revanced.patches.youtube.utils.playertype

import app.revanced.patches.youtube.utils.resourceid.actionBarSearchResultsViewMic
import app.revanced.patches.youtube.utils.resourceid.reelWatchPlayer
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val actionBarSearchResultsFingerprint = legacyFingerprint(
    name = "actionBarSearchResultsFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Landroid/view/View;",
    literals = listOf(actionBarSearchResultsViewMic),
    customFingerprint = { method, _ ->
        indexOfLayoutDirectionInstruction(method) >= 0
    }
)

internal fun indexOfLayoutDirectionInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>().toString() == "Landroid/view/View;->setLayoutDirection(I)V"
    }

internal val browseIdClassFingerprint = legacyFingerprint(
    name = "browseIdClassFingerprint",
    returnType = "Ljava/lang/Object;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    parameters = listOf("Ljava/lang/Object;", "L"),
    strings = listOf("VL")
)

internal val playerTypeFingerprint = legacyFingerprint(
    name = "playerTypeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IF_NE,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/YouTubePlayerOverlaysLayout;")
    }
)

internal val reelWatchPagerFingerprint = legacyFingerprint(
    name = "reelWatchPagerFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(reelWatchPlayer),
)

internal val videoStateFingerprint = legacyFingerprint(
    name = "videoStateFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Lcom/google/android/libraries/youtube/player/features/overlay/controls/ControlsState;"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT, // obfuscated parameter field name
        Opcode.IGET_OBJECT,
        Opcode.IF_NE,
    ),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "equals"
        } >= 0
    },
)
