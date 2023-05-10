package app.revanced.patches.youtube.layout.player.autoplaybutton.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.autoNavPreviewId
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.PLAYER
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("hide-autoplay-button")
@Description("Hides the autoplay button in the video player.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideAutoplayButtonPatch : BytecodePatch(
    listOf(LayoutConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        LayoutConstructorFingerprint.result?.mutableMethod?.let {
            val insertInstruction = it.implementation!!.instructions

            val dummyRegister = it.instruction<OneRegisterInstruction>(it.getStringIndex("1.0x")).registerA
            val insertIndex = it.getWideLiteralIndex(autoNavPreviewId)

            val branchIndex = insertInstruction.subList(insertIndex + 1, insertInstruction.size - 1).indexOfFirst { instruction ->
                ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "addOnLayoutChangeListener"
            } + 2

            val jumpInstruction = it.instruction<Instruction>(insertIndex + branchIndex)

            it.addInstructions(
                insertIndex, """
                    invoke-static {}, $PLAYER->hideAutoPlayButton()Z
                    move-result v$dummyRegister
                    if-nez v$dummyRegister, :hidden
                    """, listOf(ExternalLabel("hidden", jumpInstruction))
            )
        } ?: return LayoutConstructorFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_AUTOPLAY_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-autoplay-button")

        return PatchResultSuccess()
    }
}

