package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.DISABLE_SCREENSHOT_POPUP
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.findMutableMethodOf
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val screenshotPopupPatch = bytecodePatch(
    DISABLE_SCREENSHOT_POPUP.title,
    DISABLE_SCREENSHOT_POPUP.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        fun indexOfShowBannerInstruction(method: Method) =
            method.indexOfFirstInstruction {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IGET_OBJECT &&
                        reference?.name?.contains("shouldShowBanner") == true &&
                        reference.definingClass.startsWith("Lcom/reddit/sharing/screenshot/") == true
            }

        fun indexOfSetValueInstruction(method: Method) =
            method.indexOfFirstInstruction {
                getReference<MethodReference>()?.name == "setValue"
            }

        fun indexOfBooleanInstruction(method: Method, startIndex: Int = 0) =
            method.indexOfFirstInstruction(startIndex) {
                val reference = getReference<FieldReference>()
                opcode == Opcode.SGET_OBJECT &&
                        reference?.definingClass == "Ljava/lang/Boolean;" &&
                        reference.type == "Ljava/lang/Boolean;"
            }

        val isScreenShotMethod: Method.() -> Boolean = {
            definingClass.startsWith("Lcom/reddit/sharing/screenshot/") &&
                    name == "invokeSuspend" &&
                    indexOfShowBannerInstruction(this) >= 0 &&
                    indexOfBooleanInstruction(this) >= 0 &&
                    indexOfSetValueInstruction(this) >= 0
        }

        var hookCount = 0

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.isScreenShotMethod()) {
                    proxy(classDef)
                        .mutableClass
                        .findMutableMethodOf(method)
                        .apply {
                            val showBannerIndex = indexOfShowBannerInstruction(this)
                            val booleanIndex = indexOfBooleanInstruction(this, showBannerIndex)
                            val booleanRegister =
                                getInstruction<OneRegisterInstruction>(booleanIndex).registerA

                            addInstructions(
                                booleanIndex + 1, """
                                    invoke-static {v$booleanRegister}, $PATCHES_PATH/ScreenshotPopupPatch;->disableScreenshotPopup(Ljava/lang/Boolean;)Ljava/lang/Boolean;
                                    move-result-object v$booleanRegister
                                    """
                            )
                            hookCount++
                        }
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
