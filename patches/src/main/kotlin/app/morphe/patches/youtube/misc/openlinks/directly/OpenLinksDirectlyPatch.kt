package app.morphe.patches.youtube.misc.openlinks.directly

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.MISC_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.BYPASS_URL_REDIRECTS
import app.morphe.patches.youtube.utils.playservice.is_20_10_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_37_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_49_or_greater
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.Opcode

val bypassURLRedirectsPatch = bytecodePatch(
    BYPASS_URL_REDIRECTS.title,
    BYPASS_URL_REDIRECTS.summary,
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        if (is_20_10_or_greater) {
            arrayOf(
                HttpUriParserFingerprint to 0,

                if (is_20_49_or_greater) {
                    // Code has moved, and now seems to be an account URL
                    // and may not be anything to do with sharing links.
                    null to -1
                } else if (is_20_37_or_greater) {
                    AbUriParserFingerprint to 2
                } else {
                    AbUriParserLegacyFingerprint to 2
                }
            ).forEach { (fingerprint, index) ->
                if (fingerprint == null) return@forEach

                fingerprint.method.apply {
                    val insertIndex = fingerprint.instructionMatches[index].index
                    val uriStringRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                    replaceInstruction(
                        insertIndex,
                        "invoke-static { v$uriStringRegister }, $MISC_PATH/OpenLinksDirectlyPatch;->" +
                                "parseRedirectUri(Ljava/lang/String;)Landroid/net/Uri;",
                    )
                }
            }
        }

        if (!is_20_10_or_greater) {
            arrayOf(
                openLinksDirectlyFingerprintPrimary,
                openLinksDirectlyFingerprintSecondary
            ).forEach { fingerprint ->
                fingerprint.methodOrThrow().apply {
                    val insertIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_STATIC &&
                                getReference<MethodReference>()?.name == "parse"
                    }
                    val insertRegister =
                        getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                    replaceInstruction(
                        insertIndex,
                        "invoke-static {v$insertRegister}, $MISC_PATH/OpenLinksDirectlyPatch;->parseRedirectUri(Ljava/lang/String;)Landroid/net/Uri;"
                    )
                }
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "SETTINGS: BYPASS_URL_REDIRECTS"
            ),
            BYPASS_URL_REDIRECTS
        )

        // endregion
    }
}
