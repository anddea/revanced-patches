package app.revanced.patches.music.utils.fix.fileprovider

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.utils.fix.fileprovider.fingerprints.FileProviderResolverFingerprint
import app.revanced.patches.music.utils.gms.GmsCoreSupportResourcePatch
import app.revanced.util.resultOrThrow
import app.revanced.util.valueOrThrow

object FileProviderPatch : BytecodePatch(
    setOf(FileProviderResolverFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        val youtubePackageName = GmsCoreSupportResourcePatch.PackageNameYouTube.valueOrThrow()
        val musicPackageName = GmsCoreSupportResourcePatch.PackageNameYouTubeMusic.valueOrThrow()

        /**
         * For some reason, if the app gets "android.support.FILE_PROVIDER_PATHS",
         * the package name of YouTube is used, not the package name of the YT Music.
         *
         * There is no issue in the stock YT Music, but this is an issue in the GmsCore Build.
         * https://github.com/inotia00/ReVanced_Extended/issues/1830
         *
         * To solve this issue, replace the package name of YouTube with YT Music's package name.
         */
        FileProviderResolverFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        const-string v0, "com.google.android.youtube.fileprovider"
                        invoke-static {p1, v0}, Ljava/util/Objects;->equals(Ljava/lang/Object;Ljava/lang/Object;)Z
                        move-result v0
                        if-nez v0, :fix
                        const-string v0, "$youtubePackageName.fileprovider"
                        invoke-static {p1, v0}, Ljava/util/Objects;->equals(Ljava/lang/Object;Ljava/lang/Object;)Z
                        move-result v0
                        if-nez v0, :fix
                        goto :ignore
                        :fix
                        const-string p1, "$musicPackageName.fileprovider"
                        """, ExternalLabel("ignore", getInstruction(0))
                )
            }
        }

    }
}
