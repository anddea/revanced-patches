package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.player.components.fingerprints.AudioVideoSwitchToggleFingerprint.AUDIO_VIDEO_SWITCH_TOGGLE_VISIBILITY
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object AudioVideoSwitchToggleFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.toString() == AUDIO_VIDEO_SWITCH_TOGGLE_VISIBILITY
        } >= 0
    }
) {
    const val AUDIO_VIDEO_SWITCH_TOGGLE_VISIBILITY =
        "Lcom/google/android/apps/youtube/music/player/AudioVideoSwitcherToggleView;->setVisibility(I)V"
}