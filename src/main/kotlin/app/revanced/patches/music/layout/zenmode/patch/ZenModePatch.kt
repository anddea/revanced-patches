package app.revanced.patches.music.layout.zenmode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.layout.zenmode.fingerprints.ZenModeFingerprint
import app.revanced.patches.music.utils.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.music.utils.fingerprints.ColorMatchPlayerParentFingerprint
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("enable-zen-mode")
@Description("Adds a grey tint to the video player to reduce eye strain.")
@DependsOn([MusicSettingsPatch::class])
@MusicCompatibility
@Version("0.0.1")
class ZenModePatch : BytecodePatch(
    listOf(ColorMatchPlayerParentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        ColorMatchPlayerParentFingerprint.result?.let { parentResult ->
            ZenModeFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex

                    val firstRegister = getInstruction<OneRegisterInstruction>(startIndex).registerA
                    val secondRegister =
                        getInstruction<OneRegisterInstruction>(startIndex + 2).registerA
                    val dummyRegister = secondRegister + 1

                    val referenceIndex = it.scanResult.patternScanResult!!.endIndex + 1
                    val targetReference =
                        getInstruction<ReferenceInstruction>(referenceIndex).reference.toString()

                    val insertIndex = referenceIndex + 1

                    addInstructionsWithLabels(
                        insertIndex, """
                            invoke-static {}, $MUSIC_LAYOUT->enableZenMode()Z
                            move-result v$dummyRegister
                            if-eqz v$dummyRegister, :off
                            const v$dummyRegister, -0xfcfcfd
                            if-ne v$firstRegister, v$dummyRegister, :off
                            const v$firstRegister, -0xbfbfc0
                            const v$secondRegister, -0xbfbfc0
                            :off
                            sget-object v0, $targetReference
                            """
                    )
                    removeInstruction(referenceIndex)
                }
            } ?: return ZenModeFingerprint.toErrorResult()
        } ?: return ColorMatchPlayerParentFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(
            CategoryType.LAYOUT,
            "revanced_enable_zen_mode",
            "false"
        )

        return PatchResultSuccess()
    }
}