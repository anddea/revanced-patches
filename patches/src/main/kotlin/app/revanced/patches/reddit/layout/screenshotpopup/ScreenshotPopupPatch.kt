package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.DISABLE_SCREENSHOT_POPUP
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.findMutableMethodOf
import app.revanced.util.fingerprint.methodCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/ScreenshotPopupPatch;->disableScreenshotPopup()Z"

@Suppress("unused")
val screenshotPopupPatch = bytecodePatch(
    DISABLE_SCREENSHOT_POPUP.title,
    DISABLE_SCREENSHOT_POPUP.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        val screenshotTriggerSharingListenerMethodCall =
            screenshotBannerContainerFingerprint.methodCall()

        fun indexOfScreenshotTriggerInstruction(method: Method) =
            method.indexOfFirstInstruction {
                getReference<MethodReference>()?.toString() == screenshotTriggerSharingListenerMethodCall
            }

        val isScreenshotTriggerMethod: Method.() -> Boolean = {
            indexOfScreenshotTriggerInstruction(this) >= 0
        }

        var hookCount = 0

        fun MutableMethod.hook() {
            if (returnType == "V") {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $EXTENSION_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :shown
                        return-void
                        """, ExternalLabel("shown", getInstruction(0))
                )

                hookCount++
            } else if (returnType.startsWith("L")) { // Reddit 2025.06+
                val insertIndex =
                    indexOfFirstStringInstruction("screenshotTriggerSharingListener")

                if (insertIndex >= 0) {
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA
                    val triggerIndex =
                        indexOfScreenshotTriggerInstruction(this)
                    val jumpIndex =
                        indexOfFirstInstructionOrThrow(triggerIndex, Opcode.RETURN_OBJECT)

                    addInstructionsWithLabels(
                        insertIndex, """
                            invoke-static {}, $EXTENSION_METHOD_DESCRIPTOR
                            move-result v$insertRegister
                            if-nez v$insertRegister, :hidden
                            """, ExternalLabel("hidden", getInstruction(jumpIndex))
                    )

                    hookCount++
                }
            }
        }

        screenshotBannerContainerFingerprint
            .methodOrThrow()
            .hook()

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.isScreenshotTriggerMethod()) {
                    proxy(classDef)
                        .mutableClass
                        .findMutableMethodOf(method)
                        .hook()
                }
            }
        }

        if (hookCount == 0) {
            throw PatchException("Failed to find hook method")
        }

        updatePatchStatus(
            "enableScreenshotPopup",
            DISABLE_SCREENSHOT_POPUP
        )
    }
}
