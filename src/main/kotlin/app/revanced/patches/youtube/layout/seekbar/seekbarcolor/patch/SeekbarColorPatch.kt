package app.revanced.patches.youtube.layout.seekbar.seekbarcolor.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.ControlsOverlayStyleFingerprint
import app.revanced.patches.youtube.layout.seekbar.seekbarcolor.fingerprints.SeekbarColorFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction

@Patch
@Name("custom-seekbar-color")
@Description("Change seekbar color.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
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
        SeekbarColorFingerprint.result?.mutableMethod?.let { method ->
            with (method.implementation!!.instructions) {
                val insertIndex = this.indexOfFirst {
                    (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.timeBarPlayedDarkLabelId
                } + 2

                val insertRegister = (elementAt(insertIndex) as OneRegisterInstruction).registerA

                method.addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$insertRegister}, $SEEKBAR->enableCustomSeekbarColorDarkMode(I)I
                        move-result v$insertRegister
                        """
                )
            }
        } ?: return SeekbarColorFingerprint.toErrorResult()

        val controlsOverlayStyleClassDef = ControlsOverlayStyleFingerprint.result?.classDef?: return ControlsOverlayStyleFingerprint.toErrorResult()

        val progressColorFingerprint =
            object : MethodFingerprint(returnType = "V", parameters = listOf("I"), customFingerprint = { it.name == "e" }) {}
        progressColorFingerprint.resolve(context, controlsOverlayStyleClassDef)
        progressColorFingerprint.result?.mutableMethod?.addInstructions(
            0, """
                invoke-static {p1}, $SEEKBAR->enableCustomSeekbarColor(I)I
                move-result p1
            """
        )?: return progressColorFingerprint.toErrorResult()

        /*
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
}
