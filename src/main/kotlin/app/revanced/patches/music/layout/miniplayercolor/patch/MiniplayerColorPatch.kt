package app.revanced.patches.music.layout.miniplayercolor.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.layout.miniplayercolor.fingerprints.MiniplayerColorFingerprint
import app.revanced.patches.music.misc.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.fingerprints.MiniplayerColorParentFingerprint
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("enable-color-match-player")
@Description("Matches the fullscreen player color with the minimized one.")
@DependsOn([MusicSettingsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class MiniplayerColorPatch : BytecodePatch(
    listOf(
        MiniplayerColorFingerprint, MiniplayerColorParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        MiniplayerColorParentFingerprint.result?.let { parentResult ->

            with (parentResult.mutableMethod.implementation!!.instructions) {
                firstReference =
                    (elementAt(4) as ReferenceInstruction).reference as FieldReference

                secondReference =
                    (elementAt(5) as ReferenceInstruction).reference as FieldReference

                thirdReference =
                    (elementAt(6) as ReferenceInstruction).reference as MethodReference

                fourthReference =
                    (elementAt(10) as ReferenceInstruction).reference as FieldReference

                fifthReference =
                    (elementAt(11) as ReferenceInstruction).reference as FieldReference

                sixthReference =
                    (elementAt(12) as ReferenceInstruction).reference as MethodReference
            }

            MiniplayerColorFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                with (it.mutableMethod) {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex + 1
                    val jumpInstruction = implementation!!.instructions[insertIndex] as Instruction

                    val type = it.classDef.type

                    addInstructions(
                        insertIndex, """
                            invoke-static {}, $MUSIC_LAYOUT->enableColorMatchPlayer()Z
                            move-result v2
                            if-eqz v2, :off
                            iget v0, p0, ${type}->${firstReference.name}:${firstReference.type}
                            if-eq v0, v2, :abswitch
                            iput v2, p0, ${type}->${firstReference.name}:${firstReference.type}
                            iget-object v0, p0, ${type}->${secondReference.name}:${secondReference.type}
                            invoke-virtual {v0, v2, p2, p3}, $thirdReference
                            :abswitch
                            iget v0, p0, ${type}->${fourthReference.name}:${fourthReference.type}
                            if-eq v0, v1, :exit
                            iput v1, p0, ${type}->${fourthReference.name}:${fourthReference.type}
                            iget-object v0, p0, ${type}->${fifthReference.name}:${fifthReference.type}
                            invoke-virtual {v0, v1, p2, p3}, $sixthReference
                            goto :exit
                            :off
                            invoke-direct {p0}, ${type}->${parentResult.mutableMethod.name}()V
                            """, listOf(ExternalLabel("exit", jumpInstruction))
                    )
                    removeInstruction(insertIndex - 1)
                }
            } ?: return MiniplayerColorFingerprint.toErrorResult()
        } ?: return MiniplayerColorParentFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(CategoryType.LAYOUT, "revanced_enable_color_match_player", "true")

        return PatchResultSuccess()
    }
    private companion object {
        private lateinit var firstReference: FieldReference
        private lateinit var secondReference: FieldReference
        private lateinit var thirdReference: MethodReference

        private lateinit var fourthReference: FieldReference
        private lateinit var fifthReference: FieldReference
        private lateinit var sixthReference: MethodReference
    }
}