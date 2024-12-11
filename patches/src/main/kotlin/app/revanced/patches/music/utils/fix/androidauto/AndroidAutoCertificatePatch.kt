package app.revanced.patches.music.utils.fix.androidauto

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.CERTIFICATE_SPOOF
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.util.fingerprint.methodOrThrow

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
