package app.revanced.patches.youtube.misc.microg.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.layout.player.castbutton.resource.patch.HideCastButtonPatch
import app.revanced.patches.youtube.misc.clientspoof.resource.patch.ClientSpoofPatch
import app.revanced.patches.youtube.misc.microg.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.microg.shared.Constants.PACKAGE_NAME
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.patches.options.PatchOptions
import app.revanced.shared.util.bytecode.BytecodeHelper
import app.revanced.shared.util.microg.MicroGBytecodeHelper

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
        IntegrityCheckFingerprint,
        PrimeFingerprint,
        ServiceCheckFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        var packageName = PatchOptions.YouTube_PackageName

        // apply common microG patch
        MicroGBytecodeHelper.patchBytecode(
            context, arrayOf(
                MicroGBytecodeHelper.packageNameTransform(
                    PACKAGE_NAME,
                    "$packageName"
                )
            ),
            MicroGBytecodeHelper.PrimeMethodTransformationData(
                PrimeFingerprint,
                PACKAGE_NAME,
                "$packageName"
            ),
            listOf(
                IntegrityCheckFingerprint,
                ServiceCheckFingerprint,
                GooglePlayUtilityFingerprint,
                CastDynamiteModuleFingerprint,
                CastDynamiteModuleV2Fingerprint,
                CastContextFetchFingerprint
            )
        )

        BytecodeHelper.injectInit(context, "MicroGPatch", "checkAvailability")

        return PatchResultSuccess()
    }
}
