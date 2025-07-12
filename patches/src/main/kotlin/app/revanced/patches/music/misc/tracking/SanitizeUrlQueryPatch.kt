package app.revanced.patches.music.misc.tracking

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.SANITIZE_SHARING_LINKS
import app.revanced.patches.music.utils.playservice.is_8_05_or_greater
import app.revanced.patches.music.utils.playservice.is_8_17_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.tracking.baseSanitizeUrlQueryPatch
import app.revanced.patches.shared.tracking.hookQueryParameters
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val sanitizeUrlQueryPatch = bytecodePatch(
    SANITIZE_SHARING_LINKS.title,
    SANITIZE_SHARING_LINKS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseSanitizeUrlQueryPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        if (is_8_05_or_greater && !is_8_17_or_greater) {
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
            "revanced_sanitize_sharing_links",
            "true"
        )

        updatePatchStatus(SANITIZE_SHARING_LINKS)

    }
}
