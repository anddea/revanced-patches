package app.revanced.patches.music.utils.intenthook.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.utils.intenthook.fingerprints.FullStackTraceActivityFingerprint
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MUSIC_INTEGRATIONS_PATH

@DependsOn([SettingsPatch::class])
class IntentHookPatch : BytecodePatch(
    listOf(FullStackTraceActivityFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        FullStackTraceActivityFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    1, """
                        invoke-static {p0}, $MUSIC_INTEGRATIONS_PATH/settingsmenu/SharedPreferenceChangeListener;->initializeSettings(Landroid/app/Activity;)Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(1))
                )
            }
        } ?: throw FullStackTraceActivityFingerprint.exception

    }
}