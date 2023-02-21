package app.revanced.patches.youtube.misc.oldlayout.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.oldlayout.fingerprints.OldLayoutFingerprint
import app.revanced.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("enable-old-layout")
@Description("Spoof the YouTube client version to use the old layout.")
@YouTubeCompatibility
@Version("0.0.1")
class OldLayoutPatch : BytecodePatch(
    listOf(
        OldLayoutFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        OldLayoutFingerprint.result?.let {
            val insertIndex = it.scanResult.patternScanResult!!.startIndex

            with (it.mutableMethod) {
                val register = (this.implementation!!.instructions[insertIndex] as OneRegisterInstruction).registerA
                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$register}, $MISC_PATH/VersionOverridePatch;->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                    """
                )
            }
        } ?: return OldLayoutFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}