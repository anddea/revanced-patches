package app.revanced.patches.youtube.layout.player.autoplaybutton.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER_LAYOUT
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("hide-autoplay-button")
@Description("Hides the autoplay button in the video player.")
@DependsOn(
    [
        ResourceMappingPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideAutoplayButtonPatch : BytecodePatch(
    listOf(
            LayoutConstructorFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        // resolve the offsets such as ...
        val autoNavPreviewStubId = ResourceMappingPatch.resourceMappings.single {
            it.name == "autonav_preview_stub"
        }.id

        LayoutConstructorFingerprint.result?.mutableMethod?.let { method ->
            with (method.implementation!!.instructions) {
                val registerIndex = indexOfFirst {
                    it.opcode == Opcode.CONST_STRING &&
                            (it as BuilderInstruction21c).reference.toString() == "1.0x"
                }
                val dummyRegister = (this[registerIndex] as Instruction21c).registerA

                // where to insert the branch instructions and ...
                val insertIndex = this.indexOfFirst {
                    (it as? WideLiteralInstruction)?.wideLiteral == autoNavPreviewStubId
                }
                // where to branch away
                val branchIndex = this.subList(insertIndex + 1, this.size - 1).indexOfFirst {
                    ((it as? ReferenceInstruction)?.reference as? MethodReference)?.name == "addOnLayoutChangeListener"
                } + 2

                val jumpInstruction = this[insertIndex + branchIndex] as Instruction

                method.addInstructions(
                    insertIndex, """
                        invoke-static {}, $PLAYER_LAYOUT->hideAutoPlayButton()Z
                        move-result v$dummyRegister
                        if-nez v$dummyRegister, :hidden
                    """, listOf(ExternalLabel("hidden", jumpInstruction))
                )
            }
        } ?: return LayoutConstructorFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: OTHER_LAYOUT_SETTINGS",
                "PREFERENCE_HEADER: PLAYER",
                "SETTINGS: HIDE_AUTOPLAY_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-autoplay-button")

        return PatchResultSuccess()
    }
}

