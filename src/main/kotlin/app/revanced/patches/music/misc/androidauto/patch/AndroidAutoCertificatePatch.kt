package app.revanced.patches.music.misc.androidauto.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.androidauto.fingerprints.CertificateCheckFingerprint
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility

@Patch
@Name("certificate-spoof")
@Description("Spoofs the YouTube Music certificate for Android Auto.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class AndroidAutoCertificatePatch : BytecodePatch(
    listOf(CertificateCheckFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        CertificateCheckFingerprint.result?.mutableMethod?.addInstructions(
            0, """
                const/4 v0, 0x1
                return v0
                """
        ) ?: return CertificateCheckFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
