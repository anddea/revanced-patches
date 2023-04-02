package app.revanced.patches.youtube.layout.fullscreen.autoplaypreview.patch

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
import app.revanced.util.integrations.Constants.FULLSCREEN
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
import org.jf.dexlib2.iface.reference.FieldReference

@Patch
@Name("hide-autoplay-preview")
@Description("Hides the autoplay preview container in the fullscreen.")
@DependsOn(
    [
        ResourceMappingPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideAutoplayPreviewPatch : BytecodePatch(
    listOf(
        LayoutConstructorFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        // resolve the offsets such as ...
        val autoNavPreviewStubId = ResourceMappingPatch.resourceMappings.single {
            it.type == "id" && it.name == "autonav_preview_stub"
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

                val branchIndex = this.subList(insertIndex + 1, this.size - 1).indexOfFirst {
                    ((it as? ReferenceInstruction)?.reference as? FieldReference)?.type == "Lcom/google/android/apps/youtube/app/player/autonav/AutonavToggleController;"
                } + 1

                val jumpInstruction = this[insertIndex + branchIndex] as Instruction

                method.addInstructions(
                    insertIndex, """
                        invoke-static {}, $FULLSCREEN->hideAutoPlayPreview()Z
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
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_AUTOPLAY_PREVIEW"
            )
        )

        SettingsPatch.updatePatchStatus("hide-autoplay-preview")

        return PatchResultSuccess()
    }
}

