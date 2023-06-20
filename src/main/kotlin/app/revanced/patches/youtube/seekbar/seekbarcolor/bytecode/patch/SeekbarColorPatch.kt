package app.revanced.patches.youtube.seekbar.seekbarcolor.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.ControlsOverlayStyleFingerprint
import app.revanced.patches.youtube.seekbar.seekbarcolor.bytecode.fingerprints.ProgressColorFingerprint
import app.revanced.patches.youtube.seekbar.seekbarcolor.bytecode.fingerprints.SeekbarColorFingerprint
import app.revanced.patches.youtube.seekbar.seekbarcolor.resource.patch.SeekbarColorResourcePatch
import app.revanced.patches.youtube.utils.litho.patch.LithoThemePatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.InlineTimeBarColorizedBarPlayedColorDark
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.InlineTimeBarPlayedNotHighlightedColor
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SEEKBAR
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("custom-seekbar-color")
@Description("Change seekbar color in video player and video thumbnails.")
@DependsOn(
    [
        LithoThemePatch::class,
        SeekbarColorResourcePatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SeekbarColorPatch : BytecodePatch(
    listOf(
        ControlsOverlayStyleFingerprint,
        SeekbarColorFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        SeekbarColorFingerprint.result?.let {
            it.mutableMethod.apply {
                hook(getWideLiteralIndex(InlineTimeBarColorizedBarPlayedColorDark) + 2)
                hook(getWideLiteralIndex(InlineTimeBarPlayedNotHighlightedColor) + 2)
            }
        } ?: return SeekbarColorFingerprint.toErrorResult()

        ControlsOverlayStyleFingerprint.result?.let { parentResult ->
            ProgressColorFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.addInstructions(
                0, """
                    invoke-static {p1}, $SEEKBAR->getSeekbarClickedColorValue(I)I
                    move-result p1
                    """
            ) ?: return ProgressColorFingerprint.toErrorResult()
        } ?: return ControlsOverlayStyleFingerprint.toErrorResult()

        LithoThemePatch.injectCall("$SEEKBAR->getLithoColor(I)I")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: CUSTOM_SEEKBAR_COLOR"
            )
        )

        SettingsPatch.updatePatchStatus("custom-seekbar-color")

        return PatchResultSuccess()
    }

    private companion object {
        fun MutableMethod.hook(insertIndex: Int) {
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex + 1, """
                    invoke-static {v$insertRegister}, $SEEKBAR->overrideSeekbarColor(I)I
                    move-result v$insertRegister
                    """
            )
        }
    }
}
