package app.morphe.patches.youtube.misc.openlinks.directly

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.MISC_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.BYPASS_URL_REDIRECTS
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val openLinksDirectlyPatch = bytecodePatch(
    BYPASS_URL_REDIRECTS.title,
    BYPASS_URL_REDIRECTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

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
