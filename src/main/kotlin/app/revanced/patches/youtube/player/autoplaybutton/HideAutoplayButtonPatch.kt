package app.revanced.patches.youtube.player.autoplaybutton

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AutoNavPreviewStub
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.VideoZoomIndicatorLayout
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide autoplay button",
    description = "Adds an option to hide the autoplay button in the video player.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class,
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
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
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.43",
                "19.12.41",
                "19.13.37",
                "19.14.43",
                "19.15.36",
                "19.16.38"
            ]
        )
    ]
)
@Suppress("unused")
object HideAutoplayButtonPatch : BytecodePatch(
    setOf(LayoutConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        LayoutConstructorFingerprint.result?.let {
            it.mutableMethod.apply {
                val dummyRegister =
                    getInstruction<OneRegisterInstruction>(getStringInstructionIndex("1.0x")).registerA
                val insertIndex = getWideLiteralInstructionIndex(AutoNavPreviewStub)
                val jumpIndex = getWideLiteralInstructionIndex(VideoZoomIndicatorLayout) - 1

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $PLAYER->hideAutoPlayButton()Z
                        move-result v$dummyRegister
                        if-nez v$dummyRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
            }
        } ?: throw LayoutConstructorFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_AUTOPLAY_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("Hide autoplay button")

    }
}

