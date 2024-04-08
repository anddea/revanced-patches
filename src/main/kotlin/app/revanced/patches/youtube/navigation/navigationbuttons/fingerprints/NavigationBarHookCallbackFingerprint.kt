package app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.navigation.navigationbuttons.NavigationBarHookPatch
import app.revanced.patches.youtube.navigation.navigationbuttons.NavigationButtonsPatch
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Integrations method, used for callback into to other patches.
 * Specifically, [NavigationButtonsPatch].
 */
internal object NavigationBarHookCallbackFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    returnType = "V",
    parameters = listOf(NavigationBarHookPatch.INTEGRATIONS_NAVIGATION_BUTTON_DESCRIPTOR, "Landroid/view/View;"),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "navigationTabCreatedCallback" &&
            methodDef.definingClass == NavigationBarHookPatch.INTEGRATIONS_CLASS_DESCRIPTOR
    },
)
