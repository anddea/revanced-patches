package app.revanced.patches.music.player.newlayout

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.player.newlayout.fingerprints.NewLayoutFingerprint
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_PLAYER
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Enable new layout",
    description = "Enable new player layouts.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.15.52",
                "6.20.51",
                "6.21.51",
                "6.22.51"
            ]
        )
    ]
)
@Suppress("unused")
object NewLayoutPatch : BytecodePatch(
    setOf(NewLayoutFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        NewLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {}, $MUSIC_PLAYER->enableNewLayout()Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw NewLayoutFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_new_layout",
            "true"
        )

    }
}