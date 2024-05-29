package app.revanced.patches.youtube.player.speedoverlay.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.containsMethodReferenceNameInstructionIndex
import com.android.tools.smali.dexlib2.AccessFlags

internal object NextGenWatchLayoutFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = handler@{ methodDef, _ ->
        if (methodDef.definingClass != "Lcom/google/android/apps/youtube/app/watch/nextgenwatch/ui/NextGenWatchLayout;")
            return@handler false

        methodDef.containsMethodReferenceNameInstructionIndex("booleanValue")
    }
)