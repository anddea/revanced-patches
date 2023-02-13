package app.revanced.patches.music.audio.codecs.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.audio.codecs.fingerprints.*
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.integrations.Constants.MUSIC_SETTINGS_PATH
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
            AllCodecsFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let { result ->
                allCodecsMethod = 
                context.toMethodWalker(result.method)
                    .nextMethod(result.scanResult.patternScanResult!!.endIndex)
                    .getMethod()

            } ?: return AllCodecsFingerprint.toErrorResult()
        } ?: return AllCodecsParentFingerprint.toErrorResult()

        CodecsLockFingerprint.result?.let { result ->
            val endIndex = result.scanResult.patternScanResult!!.endIndex

            with(result.mutableMethod) {
                val register = (instruction(endIndex) as OneRegisterInstruction).registerA
                addInstructions(
                    endIndex + 1, """
                        invoke-static {}, $MUSIC_SETTINGS_PATH->enableOpusCodec()Z
                        move-result v7
                        if-eqz v7, :mp4a
                        invoke-static {}, ${allCodecsMethod.definingClass}->${allCodecsMethod.name}()Ljava/util/Set;
                        move-result-object v$register
                    """, listOf(ExternalLabel("mp4a", instruction(endIndex + 1)))
                )
            }
        } ?: return CodecsLockFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    private companion object {
        lateinit var allCodecsMethod: Method
    }
}
