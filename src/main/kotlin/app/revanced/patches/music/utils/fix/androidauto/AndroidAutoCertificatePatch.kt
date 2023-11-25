package app.revanced.patches.music.utils.fix.androidauto

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.fix.androidauto.fingerprints.CertificateCheckFingerprint

@Patch(
    name = "Certificate spoof",
    description = "Spoofs the YouTube Music certificate for Android Auto.",
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ]
)
@Suppress("unused")
object AndroidAutoCertificatePatch : BytecodePatch(
    setOf(CertificateCheckFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        CertificateCheckFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        const/4 v0, 0x1
                        return v0
                        """
                )
            }
        } ?: throw CertificateCheckFingerprint.exception

    }
}
