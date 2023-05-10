package app.revanced.patches.youtube.layout.seekbar.seekbarcolor.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.ControlsOverlayStyleFingerprint
import app.revanced.patches.youtube.layout.seekbar.seekbarcolor.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.litho.patch.LithoThemePatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.inlineTimeBarColorizedBarPlayedColorDarkId
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.inlineTimeBarPlayedNotHighlightedColorId
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.util.integrations.Constants.SEEKBAR
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("custom-seekbar-color-bytecode-patch")
@DependsOn(
    [
        LithoThemePatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SeekbarColorBytecodePatch : BytecodePatch(
    listOf(
        ControlsOverlayParentFingerprint,
        ControlsOverlayStyleFingerprint,
        SeekbarColorFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        SeekbarColorFingerprint.result?.mutableMethod?.let {
            it.hook(it.getWideLiteralIndex(inlineTimeBarColorizedBarPlayedColorDarkId) + 2)
            it.hook(it.getWideLiteralIndex(inlineTimeBarPlayedNotHighlightedColorId) + 2)
        } ?: return SeekbarColorFingerprint.toErrorResult()

        ControlsOverlayStyleFingerprint.result?.let { parentResult ->
            ProgressColorFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.addInstructions(
                0, """
                    invoke-static {p1}, $SEEKBAR->getSeekbarClickedColorValue(I)I
                    move-result p1
                    """
            ) ?: return ProgressColorFingerprint.toErrorResult()
        } ?: return ControlsOverlayStyleFingerprint.toErrorResult()


        /**
         * Context called when playing a video in the PlayStore
         * If this context is not overridden, Exception occurs when reading a value from SharedPreference (ROOT Build)
         *
         * TODO: merging into abstract integrations patch
         */
        ControlsOverlayParentFingerprint.result?.let { parentResult ->
            ControlsOverlayFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.addInstruction(
                0,
                "sput-object p1, $INTEGRATIONS_PATH/utils/ReVancedUtils;->context:Landroid/content/Context;"
            ) ?: return ControlsOverlayFingerprint.toErrorResult()
        } ?: return ControlsOverlayParentFingerprint.toErrorResult()

        LithoThemePatch.injectCall("$SEEKBAR->resumedProgressBarColor(I)I")

        return PatchResultSuccess()
    }

    private companion object {
        fun MutableMethod.hook(insertIndex: Int) {
            val insertRegister = instruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex + 1, """
                    invoke-static {v$insertRegister}, $SEEKBAR->overrideSeekbarColor(I)I
                    move-result v$insertRegister
                    """
            )
        }
    }
}
