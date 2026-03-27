package app.morphe.patches.music.utils.fix.androidauto

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.patch.PatchList.CERTIFICATE_SPOOF
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.util.fingerprint.methodOrThrow

@Suppress("unused")
val androidAutoCertificatePatch = bytecodePatch(
    CERTIFICATE_SPOOF.title,
    CERTIFICATE_SPOOF.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        certificateCheckFingerprint.methodOrThrow().addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
                """,
        )

        updatePatchStatus(CERTIFICATE_SPOOF)

    }
}
