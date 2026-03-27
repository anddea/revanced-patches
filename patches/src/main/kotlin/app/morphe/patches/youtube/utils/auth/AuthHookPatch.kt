package app.morphe.patches.youtube.utils.auth

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.extension.Constants.EXTENSION_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.request.buildRequestPatch
import app.morphe.patches.youtube.utils.request.hookBuildRequest
import app.morphe.util.fingerprint.methodOrThrow

private const val EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/innertube/utils/AuthUtils;"

val authHookPatch = bytecodePatch(
    description = "authHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        buildRequestPatch,
    )

    execute {
        // Get incognito status and data sync id.
        accountIdentityFingerprint.methodOrThrow().addInstruction(
            1,
            "invoke-static {p3, p4}, $EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setAccountIdentity(Ljava/lang/String;Z)V"
        )

        // Get the header to use the auth token.
        hookBuildRequest("$EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setRequestHeaders(Ljava/lang/String;Ljava/util/Map;)V")
    }
}
