package app.revanced.patches.music.player.zenmode.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.player.zenmode.fingerprints.ZenModeFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.fingerprints.ColorMatchPlayerParentFingerprint
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_PLAYER
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("Enable zen mode")
@Description("Adds a grey tint to the video player to reduce eye strain.")
@DependsOn([SettingsPatch::class])
@MusicCompatibility
class ZenModePatch : BytecodePatch(
    listOf(ColorMatchPlayerParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

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
                            invoke-static {}, $MUSIC_PLAYER->enableZenMode()Z
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
            } ?: throw ZenModeFingerprint.exception
        } ?: throw ColorMatchPlayerParentFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_zen_mode",
            "false"
        )

    }
}