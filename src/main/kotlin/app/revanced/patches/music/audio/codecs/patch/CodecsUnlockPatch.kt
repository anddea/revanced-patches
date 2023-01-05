package app.revanced.patches.music.audio.codecs.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.audio.codecs.fingerprints.AllCodecsReferenceFingerprint
import app.revanced.patches.music.audio.codecs.fingerprints.CodecsLockFingerprint
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.util.integrations.Constants.MUSIC_SETTINGS_PATH
import org.jf.dexlib2.Opcode

@Patch
@DependsOn([MusicIntegrationsPatch::class, MusicSettingsPatch::class])
@Name("enable-opus-codec")
@Description("Enable opus codec when playing audio.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CodecsUnlockPatch : BytecodePatch(
    listOf(
        CodecsLockFingerprint, AllCodecsReferenceFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val codecsLockResult = CodecsLockFingerprint.result!!
        val codecsLockMethod = codecsLockResult.mutableMethod

        val implementation = codecsLockMethod.implementation!!

        val scanResultStartIndex = codecsLockResult.scanResult.patternScanResult!!.startIndex
        val instructionIndex = scanResultStartIndex +
                if (implementation.instructions[scanResultStartIndex - 1].opcode == Opcode.CHECK_CAST) {
                    // for 5.16.xx and lower
                    -3
                } else {
                    // since 5.17.xx
                    -2
                }

        val allCodecsResult = AllCodecsReferenceFingerprint.result!!
        val allCodecsMethod =
            context.toMethodWalker(allCodecsResult.method)
                .nextMethod(allCodecsResult.scanResult.patternScanResult!!.startIndex)
                .getMethod()

        codecsLockMethod.addInstructions(
            instructionIndex + 2, """
                invoke-static {}, $MUSIC_SETTINGS_PATH->enableOpusCodec()Z
                move-result v7
                if-eqz v7, :mp4a
                invoke-static {}, ${allCodecsMethod.definingClass}->${allCodecsMethod.name}()Ljava/util/Set;
                move-result-object p4
                """, listOf(ExternalLabel("mp4a", codecsLockMethod.instruction(instructionIndex + 2)))
        )

        return PatchResultSuccess()
    }
}
