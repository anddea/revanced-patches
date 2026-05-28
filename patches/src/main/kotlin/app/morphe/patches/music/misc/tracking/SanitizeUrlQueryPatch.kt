package app.morphe.patches.music.misc.tracking

import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.patch.PatchList.SANITIZE_SHARING_LINKS
import app.morphe.patches.music.utils.playservice.is_8_05_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.misc.privacy.hookQueryParameters
import app.morphe.patches.shared.misc.privacy.sanitizeSharingLinksPatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val sanitizeUrlQueryPatch = sanitizeSharingLinksPatch(
    SANITIZE_SHARING_LINKS.title,
    SANITIZE_SHARING_LINKS.summary,
    block = {
        compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

        dependsOn(
            settingsPatch,
            versionCheckPatch,
        )
    },
    executeBlock = {
        if (is_8_05_or_greater) {
            imageShareLinkFormatterFingerprint.methodOrThrow().apply {
                val stringIndex = indexOfFirstStringInstructionOrThrow("android.intent.extra.TEXT")
                val insertIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.name == "putExtra" &&
                            reference.definingClass == "Landroid/content/Intent;"
                }

                hookQueryParameters(insertIndex)
            }
        }

        addSwitchPreference(
            CategoryType.MISC,
            "morphe_sanitize_sharing_links",
            "true"
        )

        addSwitchPreference(
            CategoryType.MISC,
            "morphe_replace_music_with_youtube",
            "false"
        )

        updatePatchStatus(SANITIZE_SHARING_LINKS)
    }
)
