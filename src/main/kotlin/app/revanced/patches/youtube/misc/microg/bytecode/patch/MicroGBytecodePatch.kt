package app.revanced.patches.youtube.misc.microg.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.options.PatchOptions
import app.revanced.patches.youtube.layout.player.castbutton.patch.HideCastButtonPatch
import app.revanced.patches.youtube.misc.clientspoof.patch.ClientSpoofPatch
import app.revanced.patches.youtube.misc.microg.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.microg.shared.Constants.PACKAGE_NAME
import app.revanced.util.bytecode.BytecodeHelper.injectInit
import app.revanced.util.microg.MicroGBytecodeHelper

@Name("microg-support-bytecode-patch")
@DependsOn(
    [
        ClientSpoofPatch::class,
        HideCastButtonPatch::class,
        PatchOptions::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class MicroGBytecodePatch : BytecodePatch(
    listOf(
        CastContextFetchFingerprint,
        CastDynamiteModuleFingerprint,
        CastDynamiteModuleV2Fingerprint,
        GooglePlayUtilityFingerprint,
        PrimeFingerprint,
        ServiceCheckFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        val packageName = PatchOptions.YouTubePackageName!!

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

        context.injectInit("MicroGPatch", "checkAvailability")

        return PatchResultSuccess()
    }
}
