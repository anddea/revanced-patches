package app.morphe.patches.youtube.utils.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.resourceid.offlineNoContentBodyTextNotOfflineEligible
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/AccountCredentialsInvalidTextPatch;"

val accountCredentialsInvalidTextPatch = bytecodePatch(
    description = "accountCredentialsInvalidTextPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
    )

    execute {
        // If the user recently changed their account password,
        // the app can show "You're offline. Check your internet connection."
        // even when the internet is available.  For this situation
        // YouTube + MicroG shows an offline error message.
        //
        // Change the error text to inform the user to uninstall and reinstall MicroG.
        // The user can also fix this by deleting the MicroG account but
        // MicroG accounts look almost identical to Google device accounts
        // and it's more foolproof to instead uninstall/reinstall.
        arrayOf(
            specificNetworkErrorViewControllerFingerprint,
            loadingFrameLayoutControllerFingerprint
        ).forEach { fingerprint ->
            fingerprint.methodOrThrow().apply {
                val resourceIndex = indexOfFirstLiteralInstructionOrThrow(
                    offlineNoContentBodyTextNotOfflineEligible
                )
                val getStringIndex = indexOfFirstInstructionOrThrow(resourceIndex) {
                    val reference = getReference<MethodReference>()
                    reference?.name == "getString"
                }
                val register = getInstruction<OneRegisterInstruction>(getStringIndex + 1).registerA

                addInstructions(
                    getStringIndex + 2,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->getOfflineNetworkErrorString(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register  
                    """
                )
            }
        }
    }
}
