package app.revanced.patches.music.layout.zenmode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.*
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.layout.zenmode.fingerprints.ZenModeFingerprint
import app.revanced.patches.music.misc.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.fingerprints.MiniplayerColorParentFingerprint
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference

@Patch
@Name("enable-zen-mode")
@Description("Adds a grey tint to the video player to reduce eye strain.")
@DependsOn([MusicSettingsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class ZenModePatch : BytecodePatch(
    listOf(
        ZenModeFingerprint, MiniplayerColorParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        MiniplayerColorParentFingerprint.result?.let { parentResult ->
            ZenModeFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                with (it.mutableMethod) {
                    val instructions = this.implementation!!.instructions
                    val startIndex = it.scanResult.patternScanResult!!.startIndex

                    val firstRegister = (instruction(startIndex) as OneRegisterInstruction).registerA
                    val secondRegister = (instruction(startIndex + 2) as OneRegisterInstruction).registerA
                    val dummyRegister = secondRegister + 1

                    val referenceIndex = it.scanResult.patternScanResult!!.endIndex + 1
                    val fieldReference = (instructions.elementAt(referenceIndex) as ReferenceInstruction).reference as FieldReference

                    val insertIndex = referenceIndex + 1

                    addInstructions(
                        insertIndex, """
                            invoke-static {}, $MUSIC_LAYOUT->enableZenMode()Z
                            move-result v$dummyRegister
                            if-eqz v$dummyRegister, :off
                            const v$dummyRegister, -0xfcfcfd
                            if-ne v$firstRegister, v$dummyRegister, :off
                            const v$firstRegister, -0xbfbfc0
                            const v$secondRegister, -0xbfbfc0
                            :off
                            sget-object v0, ${fieldReference.type}->${fieldReference.name}:${fieldReference.type}
                            """
                    )
                    removeInstruction(referenceIndex)
                }
            } ?: return ZenModeFingerprint.toErrorResult()
        } ?: return MiniplayerColorParentFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(CategoryType.LAYOUT, "revanced_enable_zen_mode", "false")

        return PatchResultSuccess()
    }
}