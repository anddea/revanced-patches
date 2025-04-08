package app.revanced.patches.youtube.misc.accessibility

import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.HIDE_ACCESSIBILITY_CONTROLS_DIALOG
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.or
import app.revanced.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
val accessibilityPatch = bytecodePatch(
    HIDE_ACCESSIBILITY_CONTROLS_DIALOG.title,
    HIDE_ACCESSIBILITY_CONTROLS_DIALOG.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        playerAccessibilitySettingsEduControllerParentFingerprint
            .mutableClassOrThrow()
            .methods
            .first { method -> method.name == "<init>" }
            .apply {
                val lifecycleObserverIndex =
                    indexOfFirstInstructionReversedOrThrow(Opcode.NEW_INSTANCE)
                val lifecycleObserverClass =
                    getInstruction<ReferenceInstruction>(lifecycleObserverIndex).reference.toString()

                findMethodOrThrow(lifecycleObserverClass) {
                    accessFlags == AccessFlags.PUBLIC or AccessFlags.FINAL &&
                            parameterTypes.size == 1 &&
                            indexOfFirstInstruction(Opcode.INVOKE_DIRECT) >= 0
                }.returnEarly()
            }

        addPreference(HIDE_ACCESSIBILITY_CONTROLS_DIALOG)

    }
}
