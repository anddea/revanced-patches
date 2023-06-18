package app.revanced.patches.music.misc.sleeptimerhook.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.music.misc.sleeptimerhook.fingerprints.*
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.dexbacked.reference.DexBackedFieldReference
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation

@Name("sleep-timer-hook")
@YouTubeMusicCompatibility
@Version("0.0.1")
class SleepTimerHookPatch : BytecodePatch(
    listOf(
        MusicPlaybackControlsClickFingerprint,
        SleepTimerFinalFingerprint,
        SleepTimerStaticFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val sleepTimerStaticMethod = SleepTimerStaticFingerprint.result?.mutableMethod
            ?: return SleepTimerStaticFingerprint.toErrorResult()

        val sleepTimerStaticMethodToCall = sleepTimerStaticMethod.definingClass +
                "->" +
                sleepTimerStaticMethod.name +
                "(${sleepTimerStaticMethod.parameters[0]})" +
                sleepTimerStaticMethod.returnType


        val sleepTimerFinalMethod = SleepTimerFinalFingerprint.result?.mutableMethod
            ?: return SleepTimerFinalFingerprint.toErrorResult()

        val sleepTimerFinalMethodToCall = sleepTimerFinalMethod.definingClass +
                "->" +
                sleepTimerFinalMethod.name +
                "(${sleepTimerFinalMethod.parameters[0]})" +
                sleepTimerFinalMethod.returnType


        MusicPlaybackControlsClickFingerprint.result?.let { parentResult ->
            with (parentResult.mutableMethod.implementation!!.instructions) {
                for (i in this.size - 1 downTo 0) {
                    if (this[i].opcode != Opcode.IGET_OBJECT) continue

                    val invokeInstruction = this[i] as? ReferenceInstruction ?: continue
                    val targetReference = (invokeInstruction.reference as DexBackedFieldReference).toString()
                    if (!targetReference.contains(parentResult.classDef.type)) continue
                    musicPlaybackControlsMethodToCall = targetReference
                    break
                }
                if (musicPlaybackControlsMethodToCall == null)
                    return MusicPlaybackControlsClickFingerprint.toErrorResult()
            }

            MusicPlaybackControlsTimeFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.addInstruction(
                1,
                "invoke-virtual {p0}, ${parentResult.classDef.type}->openSleepTimer()V"
            ) ?: return MusicPlaybackControlsTimeFingerprint.toErrorResult()

            parentResult.mutableClass.methods.add(
                ImmutableMethod(
                    parentResult.classDef.type,
                    "openSleepTimer",
                    listOf(),
                    "V",
                    AccessFlags.PUBLIC or AccessFlags.FINAL,
                    null,
                    null,
                    ImmutableMethodImplementation(
                        4, """
                            sget-boolean v0, $INTEGRATIONS_CLASS_DESCRIPTOR
                            if-eqz v0, :dismiss
                            iget-object v1, v3, $musicPlaybackControlsMethodToCall
                            invoke-static {v1}, $sleepTimerStaticMethodToCall
                            move-result-object v0
                            iget-object v1, v3, $musicPlaybackControlsMethodToCall
                            invoke-virtual {v0, v1}, $sleepTimerFinalMethodToCall
                            const/4 v0, 0x0
                            sput-boolean v0, $INTEGRATIONS_CLASS_DESCRIPTOR
                            :dismiss
                            return-void
                            """.toInstructions(), null, null
                    )
                ).toMutable()
            )
        } ?: return MusicPlaybackControlsClickFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR = "$MUSIC_MISC_PATH/HookShareButtonPatch;->isShareButtonClicked:Z"
        var musicPlaybackControlsMethodToCall: String? = null
    }
}
