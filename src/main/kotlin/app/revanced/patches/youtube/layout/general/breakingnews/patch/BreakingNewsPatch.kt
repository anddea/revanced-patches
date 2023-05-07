package app.revanced.patches.youtube.layout.general.breakingnews.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.breakingnews.fingerprints.BreakingNewsFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("hide-breaking-news-shelf")
@Description("Hides the breaking news shelf on the homepage tab.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class,
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class BreakingNewsPatch : BytecodePatch(
    listOf(
        BreakingNewsFingerprint,
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        BreakingNewsFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = (instruction(targetIndex) as OneRegisterInstruction).registerA
                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $GENERAL->hideBreakingNewsShelf(Landroid/view/View;)V"
                )
            }
        }?: return BreakingNewsFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_BREAKING_NEWS_SHELF"
            )
        )

        SettingsPatch.updatePatchStatus("hide-breaking-news-shelf")

        return PatchResultSuccess()
    }
}
