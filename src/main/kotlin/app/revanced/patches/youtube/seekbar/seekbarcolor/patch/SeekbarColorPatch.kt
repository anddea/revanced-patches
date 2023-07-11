package app.revanced.patches.youtube.seekbar.seekbarcolor.patch

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
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.seekbar.seekbarcolor.fingerprints.ControlsOverlayStyleFingerprint
import app.revanced.patches.youtube.seekbar.seekbarcolor.fingerprints.ProgressColorFingerprint
import app.revanced.patches.youtube.seekbar.seekbarcolor.fingerprints.SeekbarColorFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.litho.patch.LithoThemePatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.InlineTimeBarColorizedBarPlayedColorDark
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.InlineTimeBarPlayedNotHighlightedColor
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch.Companion.contexts
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SEEKBAR
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

@Patch
@Name("Custom seekbar color")
@Description("Change seekbar color in video player and video thumbnails.")
@DependsOn(
    [
        LithoThemePatch::class,
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
            ProgressColorFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.mutableMethod?.addInstructions(
                0, """
                    invoke-static {p1}, $SEEKBAR->getSeekbarClickedColorValue(I)I
                    move-result p1
                    """
            ) ?: return ProgressColorFingerprint.toErrorResult()
        } ?: return ControlsOverlayStyleFingerprint.toErrorResult()

        LithoThemePatch.injectCall("$SEEKBAR->getLithoColor(I)I")

        contexts.xmlEditor["res/drawable/resume_playback_progressbar_drawable.xml"].use {
            val layerList = it.file.getElementsByTagName("layer-list").item(0) as Element
            val progressNode = layerList.getElementsByTagName("item").item(1) as Element
            if (!progressNode.getAttributeNode("android:id").value.endsWith("progress")) {
                return PatchResultError("Could not find progress bar")
            }
            val scaleNode = progressNode.getElementsByTagName("scale").item(0) as Element
            val shapeNode = scaleNode.getElementsByTagName("shape").item(0) as Element
            val replacementNode = it.file.createElement(
                "app.revanced.integrations.patches.utils.ProgressBarDrawable"
            )
            scaleNode.replaceChild(replacementNode, shapeNode)
        }

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
