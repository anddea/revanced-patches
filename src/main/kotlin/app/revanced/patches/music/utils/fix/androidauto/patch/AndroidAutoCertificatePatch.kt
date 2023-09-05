package app.revanced.patches.music.utils.fix.androidauto.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.fix.androidauto.fingerprints.CertificateCheckFingerprint

@Patch
@Name("Certificate spoof")
@Description("Spoofs the YouTube Music certificate for Android Auto.")
@MusicCompatibility
class AndroidAutoCertificatePatch : BytecodePatch(
    listOf(CertificateCheckFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        CertificateCheckFingerprint.result?.mutableMethod?.addInstructions(
            0, """
                const/4 v0, 0x1
                return v0
                """
        ) ?: throw CertificateCheckFingerprint.exception

    }
}
