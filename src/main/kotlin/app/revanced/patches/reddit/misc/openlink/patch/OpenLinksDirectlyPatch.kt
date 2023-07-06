package app.revanced.patches.reddit.misc.openlink.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.misc.openlink.fingerprints.ScreenNavigatorFingerprint
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("open-links-directly")
@Description("Skips over redirection URLs to external links.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Version("0.0.1")
class OpenLinksDirectlyPatch : BytecodePatch(
    listOf(ScreenNavigatorFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ScreenNavigatorFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        invoke-static {p2}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        move-result-object p2
                        """
                )
            }
        } ?: return ScreenNavigatorFingerprint.toErrorResult()

        updateSettingsStatus("OpenLinksDirectly")

        return PatchResultSuccess()
    }

    private companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/OpenLinksDirectlyPatch;" +
                    "->parseRedirectUri(Landroid/net/Uri;)Landroid/net/Uri;"
    }
}