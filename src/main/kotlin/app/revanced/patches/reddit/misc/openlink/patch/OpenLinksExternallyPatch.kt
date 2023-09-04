package app.revanced.patches.reddit.misc.openlink.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.misc.openlink.fingerprints.ScreenNavigatorFingerprint
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getStringIndex

@Patch
@Name("Open links externally")
@Description("Open links outside of the app directly in your browser.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
class OpenLinksExternallyPatch : BytecodePatch(
    listOf(ScreenNavigatorFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ScreenNavigatorFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getStringIndex("uri") + 2

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {p1, p2}, $INTEGRATIONS_METHOD_DESCRIPTOR->openLinksExternally(Landroid/app/Activity;Landroid/net/Uri;)Z
                        move-result v0
                        if-eqz v0, :dismiss
                        return-void
                        """, ExternalLabel("dismiss", getInstruction(insertIndex))
                )
            }
        } ?: throw ScreenNavigatorFingerprint.exception

        updateSettingsStatus("OpenLinksExternally")

    }

    private companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/OpenLinksExternallyPatch;"
    }
}