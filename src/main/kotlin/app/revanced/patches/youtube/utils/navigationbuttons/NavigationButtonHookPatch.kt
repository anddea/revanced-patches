package app.revanced.patches.youtube.utils.navigationbuttons

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch

@Patch(dependencies = [SharedResourceIdPatch::class])
object NavigationButtonHookPatch : BytecodePatch(
    setOf(PivotBarCreateButtonViewFingerprint)
) {
    internal lateinit var PivotBarResult: MethodFingerprintResult
    internal lateinit var PivotBarMethod: MutableMethod

    override fun execute(context: BytecodeContext) {
        PivotBarResult = PivotBarCreateButtonViewFingerprint.result
            ?: throw PivotBarCreateButtonViewFingerprint.exception

        PivotBarMethod = PivotBarResult.mutableMethod
    }
}