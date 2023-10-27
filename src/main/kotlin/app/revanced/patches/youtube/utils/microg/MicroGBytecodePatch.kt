package app.revanced.patches.youtube.utils.microg

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.packagename.PackageNamePatch
import app.revanced.patches.youtube.utils.fix.clientspoof.ClientSpoofPatch
import app.revanced.patches.youtube.utils.fix.parameter.SpoofPlayerParameterPatch
import app.revanced.patches.youtube.utils.microg.Constants.PACKAGE_NAME
import app.revanced.patches.youtube.utils.microg.fingerprints.CastContextFetchFingerprint
import app.revanced.patches.youtube.utils.microg.fingerprints.CastDynamiteModuleFingerprint
import app.revanced.patches.youtube.utils.microg.fingerprints.CastDynamiteModuleV2Fingerprint
import app.revanced.patches.youtube.utils.microg.fingerprints.GooglePlayUtilityFingerprint
import app.revanced.patches.youtube.utils.microg.fingerprints.PrimeFingerprint
import app.revanced.patches.youtube.utils.microg.fingerprints.ServiceCheckFingerprint
import app.revanced.util.bytecode.BytecodeHelper.injectInit
import app.revanced.util.microg.MicroGBytecodeHelper

@Patch(
    dependencies = [
        ClientSpoofPatch::class,
        PackageNamePatch::class,
        SpoofPlayerParameterPatch::class
    ]
)
object MicroGBytecodePatch : BytecodePatch(
    setOf(
        CastContextFetchFingerprint,
        CastDynamiteModuleFingerprint,
        CastDynamiteModuleV2Fingerprint,
        GooglePlayUtilityFingerprint,
        PrimeFingerprint,
        ServiceCheckFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        val packageName = PackageNamePatch.PackageNameYouTube
            ?: throw PatchException("Invalid package name.")

        if (packageName == PACKAGE_NAME)
            throw PatchException("Original package name is not available as package name for MicroG build.")

        // apply common microG patch
        MicroGBytecodeHelper.patchBytecode(
            context, arrayOf(
                MicroGBytecodeHelper.packageNameTransform(
                    PACKAGE_NAME,
                    packageName
                )
            ),
            MicroGBytecodeHelper.PrimeMethodTransformationData(
                PrimeFingerprint,
                PACKAGE_NAME,
                packageName
            ),
            listOf(
                ServiceCheckFingerprint,
                GooglePlayUtilityFingerprint,
                CastDynamiteModuleFingerprint,
                CastDynamiteModuleV2Fingerprint,
                CastContextFetchFingerprint
            )
        )

        context.injectInit("MicroGPatch", "checkAvailability", true)

    }
}
