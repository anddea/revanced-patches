package app.revanced.patches.music.misc.codecs.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.misc.codecs.fingerprints.AllCodecsFingerprint
import app.revanced.patches.music.misc.codecs.fingerprints.AllCodecsParentFingerprint
import app.revanced.patches.music.misc.codecs.fingerprints.CodecsLockFingerprint
import app.revanced.patches.music.misc.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-opus-codec")
@Description("Enable opus codec when playing audio.")
@DependsOn([MusicSettingsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class CodecsUnlockPatch : BytecodePatch(
    listOf(
        AllCodecsParentFingerprint,
        CodecsLockFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        AllCodecsParentFingerprint.result?.let { parentResult ->
            AllCodecsFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                allCodecsMethod = context.toMethodWalker(it.method)
                    .nextMethod(it.scanResult.patternScanResult!!.endIndex)
                    .getMethod()

            } ?: return AllCodecsFingerprint.toErrorResult()
        } ?: return AllCodecsParentFingerprint.toErrorResult()

        CodecsLockFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA
                addInstructionsWithLabels(
                    targetIndex + 1, """
                        invoke-static {}, $MUSIC_MISC_PATH/OpusCodecPatch;->enableOpusCodec()Z
                        move-result v7
                        if-eqz v7, :mp4a
                        invoke-static {}, ${allCodecsMethod.definingClass}->${allCodecsMethod.name}()Ljava/util/Set;
                        move-result-object v$targetRegister
                        """, ExternalLabel("mp4a", getInstruction(targetIndex + 1))
                )
            }
        } ?: return CodecsLockFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(CategoryType.MISC, "revanced_enable_opus_codec", "true")

        return PatchResultSuccess()
    }

    private companion object {
        lateinit var allCodecsMethod: Method
    }
}
