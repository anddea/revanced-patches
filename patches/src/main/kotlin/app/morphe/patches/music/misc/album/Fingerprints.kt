package app.morphe.patches.music.misc.album

import app.morphe.patches.music.utils.resourceid.musicSnackbarActionColor
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val audioVideoSwitchToggleConstructorFingerprint = legacyFingerprint(
    name = "audioVideoSwitchToggleConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    opcodes = listOf(Opcode.INVOKE_DIRECT),
    customFingerprint = { method, _ ->
        indexOfAudioVideoSwitchSetOnClickListenerInstruction(method) >= 0
    }
)

internal fun indexOfAudioVideoSwitchSetOnClickListenerInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()
                    ?.toString()
                    ?.endsWith("/AudioVideoSwitcherToggleView;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V") == true
    }

internal val snackBarAttributeFingerprint = legacyFingerprint(
    name = "snackBarAttributeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L", "Z", "I"),
    literals = listOf(musicSnackbarActionColor),
)

internal val snackBarFingerprint = legacyFingerprint(
    name = "snackBarFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("L"),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "addOnAttachStateChangeListener"
        } >= 0
    }
)
