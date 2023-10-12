package app.revanced.patches.youtube.shorts.commentpopuppanels

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.shorts.commentpopuppanels.fingerprints.ReelWatchFragmentBuilderFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getWide32LiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Enable new comment popup panels",
    description = "Enables a new type of comment popup panel in the shorts player.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39"
            ]
        )
    ]
)
@Suppress("unused")
object NewCommentPopupPanelsPatch : BytecodePatch(
    setOf(ReelWatchFragmentBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ReelWatchFragmentBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWide32LiteralIndex(45401415) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {}, $SHORTS->enableNewCommentPopupPanels()Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw ReelWatchFragmentBuilderFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_NEW_COMMENT_POPUP_PANELS"
            )
        )

        SettingsPatch.updatePatchStatus("enable-new-comment-popup-panels")

    }
}
