package app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

/**
 * In YouTube v18.40.34+, |segmented_like_dislike_button.eml| is no longer used by some accounts (a/b tests).
 * https://github.com/ReVanced/revanced-patches/issues/2904
 *
 * I suspect this is due to a new type of SpannableString called 'RollingNumber' in YouTube's internal code.
 * No in-depth reverse engineering has been done on this yet.
 *
 * After installing the app for the first time, the app version is spoofed to v18.39.41 for about 500ms before the restart dialog is shown.
 * By doing this we can bypass these a/b tests being applied.
 */
object SpoofAppVersionPatchFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lapp/revanced/integrations/patches/misc/SpoofAppVersionPatch;"
                && methodDef.name == "getVersionOverride"
    }
)