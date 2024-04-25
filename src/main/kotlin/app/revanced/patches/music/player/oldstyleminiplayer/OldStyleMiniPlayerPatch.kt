package app.revanced.patches.music.player.oldstyleminiplayer

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.player.oldstyleminiplayer.fingerprints.MiniPlayerParentFingerprint
import app.revanced.patches.music.player.oldstyleminiplayer.fingerprints.NextButtonVisibilityFingerprint
import app.revanced.patches.music.player.oldstyleminiplayer.fingerprints.SwipeToCloseFingerprint
import app.revanced.patches.music.utils.integrations.Constants.PLAYER
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Enable old style miniplayer",
    description = "Adds an option to return the miniplayer to the old style. Deprecated in YT Music 6.42.52+.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52",
                "6.34.52",
                "6.35.52",
                "6.36.54",
                "6.37.53",
                "6.39.50",
                "6.40.53",
                "6.41.59"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object OldStyleMiniPlayerPatch : BytecodePatch(
    setOf(
        MiniPlayerParentFingerprint,
        SwipeToCloseFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        MiniPlayerParentFingerprint.result?.let { parentResult ->
            NextButtonVisibilityFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$targetRegister}, $PLAYER->enableOldStyleMiniPlayer(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
            } ?: throw PatchException("This version is not supported. Please use YT Music 6.42 or earlier.")
        } ?: throw MiniPlayerParentFingerprint.exception

        SwipeToCloseFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$targetRegister}, $PLAYER->enableOldStyleMiniPlayer(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw SwipeToCloseFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_old_style_mini_player",
            "true"
        )

    }
}