package app.revanced.shared.patches.timebar

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.fingerprints.EmptyColorFingerprint
import app.revanced.shared.fingerprints.OnDrawFingerprint
import app.revanced.shared.fingerprints.TimebarFingerprint
import app.revanced.shared.extensions.toErrorResult

@Name("hook-timebar-patch")
@DependsOn([SharedResourcdIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HookTimebarPatch : BytecodePatch(
    listOf(
        EmptyColorFingerprint,
        TimebarFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        EmptyColorFingerprintResult = EmptyColorFingerprint.result ?: return EmptyColorFingerprint.toErrorResult()

        OnDrawFingerprint.resolve(context, EmptyColorFingerprintResult.classDef)
        SetTimbarFingerprintResult = OnDrawFingerprint.result ?: return OnDrawFingerprint.toErrorResult()

        TimbarFingerprintResult = TimebarFingerprint.result ?: return TimebarFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    internal companion object {
        lateinit var EmptyColorFingerprintResult: MethodFingerprintResult
        lateinit var SetTimbarFingerprintResult: MethodFingerprintResult
        lateinit var TimbarFingerprintResult: MethodFingerprintResult
    }
}