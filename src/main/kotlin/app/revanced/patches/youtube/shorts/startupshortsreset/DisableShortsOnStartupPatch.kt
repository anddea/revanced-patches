package app.revanced.patches.youtube.shorts.startupshortsreset

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints.UserWasInShortsFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getWide32LiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Disable Shorts on startup",
    description = "Disables playing YouTube Shorts when launching YouTube.",
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
                "18.36.39",
                "18.37.36"
            ]
        )
    ]
)
@Suppress("unused")
object DisableShortsOnStartupPatch : BytecodePatch(
    setOf(UserWasInShortsFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        UserWasInShortsFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getWide32LiteralIndex(45381394)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructionsWithLabels(
                    insertIndex,
                    """
                        invoke-static { }, $SHORTS->disableStartupShortsPlayer()Z
                        move-result v$insertRegister
                        if-eqz v$insertRegister, :show_startup_shorts_player
                        return-void
                        """,
                    ExternalLabel("show_startup_shorts_player", getInstruction(insertIndex))
                )
            }
        } ?: throw UserWasInShortsFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SHORTS_SETTINGS",
                "SETTINGS: SHORTS_PLAYER_PARENT",
                "SETTINGS: DISABLE_STARTUP_SHORTS_PLAYER"
            )
        )

        SettingsPatch.updatePatchStatus("disable-startup-shorts-player")

    }
}
