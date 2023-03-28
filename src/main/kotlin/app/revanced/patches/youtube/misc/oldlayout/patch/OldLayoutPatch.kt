package app.revanced.patches.youtube.misc.oldlayout.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.options.PatchOptions
import app.revanced.patches.shared.patch.versionspoof.GeneralVersionSpoofPatch
import app.revanced.patches.youtube.misc.oldlayout.fingerprints.VersionOverrideFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH

@Patch
@Name("enable-old-layout")
@Description("Spoof the YouTube client version to use the old layout.")
@DependsOn(
    [
        GeneralVersionSpoofPatch::class,
        PatchOptions::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class OldLayoutPatch : BytecodePatch(
    listOf(
        VersionOverrideFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        GeneralVersionSpoofPatch.injectSpoof("$MISC_PATH/VersionOverridePatch;->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;")

        val clientSpoofVersion = PatchOptions.clientSpoofVersion!!

        VersionOverrideFingerprint.result?.let {
            val insertIndex = it.scanResult.patternScanResult!!.endIndex

            it.mutableMethod.replaceInstruction(insertIndex, "const-string p0, \"$clientSpoofVersion\"")

        } ?: return VersionOverrideFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_OLD_LAYOUT"
            )
        )

        SettingsPatch.updatePatchStatus("enable-old-layout")

        return PatchResultSuccess()
    }
}