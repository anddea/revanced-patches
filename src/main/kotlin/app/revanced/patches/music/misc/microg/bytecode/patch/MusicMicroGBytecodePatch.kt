package app.revanced.patches.music.misc.microg.bytecode.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.clientspoof.patch.ClientSpoofMusicPatch
import app.revanced.patches.music.misc.microg.bytecode.fingerprints.*
import app.revanced.patches.music.misc.microg.resource.patch.MusicMicroGResourcePatch
import app.revanced.patches.music.misc.microg.shared.Constants.MUSIC_PACKAGE_NAME
import app.revanced.patches.music.misc.microg.shared.Constants.YOUTUBE_PACKAGE_NAME
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.options.PatchOptions
import app.revanced.util.microg.MicroGBytecodeHelper

@Patch
@DependsOn(
    [
        ClientSpoofMusicPatch::class,
        MusicMicroGResourcePatch::class,
        PatchOptions::class
    ]
)
@Name("music-microg-support")
@Description("Allows YouTube Music ReVanced to run without root and under a different package name.")
@YouTubeMusicCompatibility
@Version("0.0.2")
class MusicMicroGBytecodePatch : BytecodePatch(
    listOf(
        ServiceCheckFingerprint,
        GooglePlayUtilityFingerprint,
        CastDynamiteModuleFingerprint,
        CastDynamiteModuleV2Fingerprint,
        CastContextFetchFingerprint,
        PrimeFingerprint,
    )
) {
    // NOTE: the previous patch also replaced the following strings, but it seems like they are not needed:
    // - "com.google.android.gms.chimera.GmsIntentOperationService",
    // - "com.google.android.gms.phenotype.internal.IPhenotypeCallbacks",
    // - "com.google.android.gms.phenotype.internal.IPhenotypeService",
    // - "com.google.android.gms.phenotype.PACKAGE_NAME",
    // - "com.google.android.gms.phenotype.UPDATE",
    // - "com.google.android.gms.phenotype",
    override fun execute(context: BytecodeContext): PatchResult {
        val packageNameYouTube = PatchOptions.YouTubePackageName!!
        val packageNameMusic = PatchOptions.MusicPackageName!!

        // apply common microG patch
        MicroGBytecodeHelper.patchBytecode(
            context,
            arrayOf(
                MicroGBytecodeHelper.packageNameTransform(
                    YOUTUBE_PACKAGE_NAME,
                    packageNameYouTube
                )
            ),
            MicroGBytecodeHelper.PrimeMethodTransformationData(
                PrimeFingerprint,
                MUSIC_PACKAGE_NAME,
                packageNameMusic
            ),
            listOf(
                ServiceCheckFingerprint,
                GooglePlayUtilityFingerprint,
                CastDynamiteModuleFingerprint,
                CastDynamiteModuleV2Fingerprint,
                CastContextFetchFingerprint
            )
        )

        return PatchResultSuccess()
    }
}
