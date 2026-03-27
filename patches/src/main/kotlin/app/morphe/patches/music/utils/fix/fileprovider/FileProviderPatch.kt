package app.morphe.patches.music.utils.fix.fileprovider

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.fingerprint.methodOrThrow

fun fileProviderPatch(
    youtubePackageName: String,
    musicPackageName: String
) = bytecodePatch(
    description = "fileProviderPatch"
) {
    execute {

        /**
         * For some reason, if the app gets "android.support.FILE_PROVIDER_PATHS",
         * the package name of YouTube is used, not the package name of the YT Music.
         *
         * There is no issue in the stock YT Music, but this is an issue in the GmsCore Build.
         * https://github.com/inotia00/ReVanced_Extended/issues/1830
         *
         * To solve this issue, replace the package name of YouTube with YT Music's package name.
         */
        fileProviderResolverFingerprint.methodOrThrow().apply {
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