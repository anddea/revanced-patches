package app.revanced.patches.youtube.utils.integrations.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.shared.integrations.BaseIntegrationsPatch.IntegrationsFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * For embedded playback.  Likely covers Google Play store and other Google products.
 */
internal object RemoteEmbedFragmentFingerprint : IntegrationsFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "L", "L"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lcom/google/android/apps/youtube/embeddedplayer/service/jar/client/RemoteEmbedFragment;"
    },
    contextRegisterResolver = { "p1" }
)