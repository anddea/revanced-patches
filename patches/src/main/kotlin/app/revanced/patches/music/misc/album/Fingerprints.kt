package app.revanced.patches.music.misc.album

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
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
                getReference<MethodReference>()?.toString() == "Lcom/google/android/apps/youtube/music/player/AudioVideoSwitcherToggleView;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V"
    }

internal val snackBarParentFingerprint = legacyFingerprint(
    name = "snackBarParentFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("L"),
    strings = listOf("No suitable parent found from the given view. Please provide a valid view.")
)
