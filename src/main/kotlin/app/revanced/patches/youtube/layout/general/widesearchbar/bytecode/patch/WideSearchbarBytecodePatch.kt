package app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints.WideSearchbarOneFingerprint
import app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints.WideSearchbarOneParentFingerprint
import app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints.WideSearchbarTwoFingerprint
import app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints.WideSearchbarTwoParentFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT

@Name("enable-wide-searchbar-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class WideSearchbarBytecodePatch : BytecodePatch(
    listOf(
        WideSearchbarOneParentFingerprint, WideSearchbarTwoParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        WideSearchbarOneFingerprint.resolve(context, WideSearchbarOneParentFingerprint.result!!.classDef)
        WideSearchbarTwoFingerprint.resolve(context, WideSearchbarTwoParentFingerprint.result!!.classDef)

        val resultOne = WideSearchbarOneFingerprint.result
        val targetMethodOne =
            context
            .toMethodWalker(resultOne!!.method)
            .nextMethod(resultOne.scanResult.patternScanResult!!.endIndex, true)
            .getMethod() as MutableMethod


        val resultTwo = WideSearchbarTwoFingerprint.result
        val targetMethodTwo =
            context.toMethodWalker(resultTwo!!.method)
            .nextMethod(resultTwo.scanResult.patternScanResult!!.startIndex, true)
            .getMethod() as MutableMethod

        injectSearchBarHook(targetMethodOne)
        injectSearchBarHook(targetMethodTwo)

        return PatchResultSuccess()
    }

    private fun injectSearchBarHook(method: MutableMethod) {
        val index = method.implementation!!.instructions.size - 1
        method.addInstructions(
            index, """
            invoke-static {}, $GENERAL_LAYOUT->enableWideSearchbar()Z
            move-result p0
        """
        )
    }
}
