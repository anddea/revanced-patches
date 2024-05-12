package app.revanced.patches.music.misc.minimizedplayback

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.misc.minimizedplayback.fingerprints.BackgroundPlaybackFingerprint
import app.revanced.patches.music.misc.minimizedplayback.fingerprints.DataSavingSettingsFragmentFingerprint
import app.revanced.patches.music.misc.minimizedplayback.fingerprints.MinimizedPlaybackManagerFingerprint
import app.revanced.patches.music.misc.minimizedplayback.fingerprints.MusicBrowserServiceFingerprint
import app.revanced.patches.music.misc.minimizedplayback.fingerprints.PodCastConfigFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getWalkerMethod
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
object MinimizedPlaybackPatch : BaseBytecodePatch(
    name = "Enable minimized playback",
    description = "Enables minimized and background playback.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        BackgroundPlaybackFingerprint,
        DataSavingSettingsFragmentFingerprint,
        MinimizedPlaybackManagerFingerprint,
        MusicBrowserServiceFingerprint,
        PodCastConfigFingerprint,
    )
) {
    override fun execute(context: BytecodeContext) {

        // region patch for background play

        BackgroundPlaybackFingerprint.resultOrThrow().mutableMethod.addInstructions(
            0, """
                const/4 v0, 0x1
                return v0
                """
        )

        // endregion

        // region patch for exclusive audio playback

        // don't play music video
        MusicBrowserServiceFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex =
                    getStringInstructionIndex("MBS: Return empty root for client: %s, isFullMediaBrowserEnabled: %b, is client browsable: %b, isRedAccount: %b")

                for (index in targetIndex downTo 0) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_VIRTUAL) continue

                    val targetReference = getInstruction<ReferenceInstruction>(index).reference

                    if (!targetReference.toString().endsWith("()Z")) continue

                    val walkerMethod = getWalkerMethod(context, index)

                    walkerMethod.addInstructions(
                        0, """
                            const/4 v0, 0x1
                            return v0
                            """
                    )
                    break
                }
            }
        }

        // don't play podcast videos
        PodCastConfigFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        // don't play podcast videos
        DataSavingSettingsFragmentFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getStringInstructionIndex("pref_key_dont_play_nma_video") + 4
                val targetRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstruction(
                    insertIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        // endregion

        // region patch for minimized playback

        MinimizedPlaybackManagerFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0, "return-void"
        )

        // endregion

    }
}
