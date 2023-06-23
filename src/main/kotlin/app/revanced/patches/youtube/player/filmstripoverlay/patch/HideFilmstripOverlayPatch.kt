package app.revanced.patches.youtube.player.filmstripoverlay.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayConfigFingerprint
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayInteractionFingerprint
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayParentFingerprint
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayPreviewFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("hide-filmstrip-overlay")
@Description("Hide filmstrip overlay on swipe controls.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideFilmstripOverlayPatch : BytecodePatch(
    listOf(
        FilmStripOverlayParentFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        FilmStripOverlayParentFingerprint.result?.classDef?.let { classDef ->
            arrayOf(
                FilmStripOverlayConfigFingerprint,
                FilmStripOverlayInteractionFingerprint,
                FilmStripOverlayPreviewFingerprint
            ).forEach { fingerprint ->
                fingerprint.also {
                    it.resolve(
                        context,
                        classDef
                    )
                }.result?.mutableMethod?.injectHook()
                    ?: return fingerprint.toErrorResult()
            }
        } ?: return FilmStripOverlayParentFingerprint.toErrorResult()

        YouTubeControlsOverlayFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getIndex("bringChildToFront") + 1
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                val jumpIndex = getIndex("setOnClickListener") + 3

                val fixIndex =
                    implementation!!.instructions.indexOfFirst { instruction -> (instruction as? NarrowLiteralInstruction)?.narrowLiteral == 12 }
                val fixRegister = getInstruction<OneRegisterInstruction>(fixIndex).registerA

                addInstructionsWithLabels(
                    insertIndex, """
                        const/16 v$fixRegister, 0xc
                        invoke-static {}, $PLAYER->hideFilmstripOverlay()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
            }
        } ?: return YouTubeControlsOverlayFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_FILMSTRIP_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("hide-filmstrip-overlay")

        return PatchResultSuccess()
    }

    private companion object {
        fun MutableMethod.injectHook() {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER->hideFilmstripOverlay()Z
                    move-result v0
                    if-eqz v0, :shown
                    const/4 v0, 0x0
                    return v0
                    """, ExternalLabel("shown", getInstruction(0))
            )
        }

        fun MutableMethod.getIndex(methodName: String): Int {
            return implementation!!.instructions.indexOfFirst { instruction ->
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@indexOfFirst false

                return@indexOfFirst ((instruction as Instruction35c).reference as MethodReference).name == methodName
            }
        }
    }
}
