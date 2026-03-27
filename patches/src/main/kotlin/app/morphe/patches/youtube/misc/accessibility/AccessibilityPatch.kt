package app.morphe.patches.youtube.misc.accessibility

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_ACCESSIBILITY_CONTROLS_DIALOG
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.or
import app.morphe.util.returnEarly
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
