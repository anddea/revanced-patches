package app.revanced.patches.youtube.misc.returnyoutubedislike.shorts.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.returnyoutubedislike.shorts.fingerprints.ShortsTextComponentParentFingerprint
import app.revanced.util.integrations.Constants.UTILS_PATH
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("return-youtube-dislike-shorts")
@YouTubeCompatibility
@Version("0.0.1")
class ReturnYouTubeDislikeShortsPatch : BytecodePatch(
    listOf(ShortsTextComponentParentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsTextComponentParentFingerprint.result?.let {
            (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod
            ).apply {
                val insertIndex = implementation!!.instructions.size - 1
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                this.insertShorts(insertIndex, insertRegister)
            }

            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex + 2
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                this.insertShorts(insertIndex, insertRegister)
            }
        } ?: return ShortsTextComponentParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
    private companion object {
        const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
            "$UTILS_PATH/ReturnYouTubeDislikePatch;"
    }

    private fun MutableMethod.insertShorts(index: Int, register: Int) {
        addInstructions(
            index, """
                invoke-static {v$register}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onShortsComponentCreated(Landroid/text/Spanned;)Landroid/text/Spanned;
                move-result-object v$register
                """
        )
    }
}
