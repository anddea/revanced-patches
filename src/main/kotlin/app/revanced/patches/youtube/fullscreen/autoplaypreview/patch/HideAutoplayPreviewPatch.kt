package app.revanced.patches.youtube.fullscreen.autoplaypreview.patch

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
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.AutoNavPreviewStub
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.AutoNavToggle
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.FULLSCREEN
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide autoplay preview")
@Description("Hides the autoplay preview container in the fullscreen.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class HideAutoplayPreviewPatch : BytecodePatch(
    listOf(LayoutConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        LayoutConstructorFingerprint.result?.let {
            it.mutableMethod.apply {
                val dummyRegister =
                    getInstruction<OneRegisterInstruction>(getStringIndex("1.0x")).registerA
                val insertIndex = getWideLiteralIndex(AutoNavPreviewStub)
                val jumpIndex = getWideLiteralIndex(AutoNavToggle) - 1

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $FULLSCREEN->hideAutoPlayPreview()Z
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
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_AUTOPLAY_PREVIEW"
            )
        )

        SettingsPatch.updatePatchStatus("hide-autoplay-preview")

    }
}

