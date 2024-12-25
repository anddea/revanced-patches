package app.revanced.patches.music.layout.overlayfilter

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.patch.PatchList.HIDE_OVERLAY_FILTER
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val overlayFilterBytecodePatch = bytecodePatch(
    description = "overlayFilterBytecodePatch"
) {
    dependsOn(sharedResourceIdPatch)

    execute {
        designBottomSheetDialogFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex - 1
                val freeRegister = getInstruction<OneRegisterInstruction>(insertIndex + 1).registerA

                addInstructions(
                    insertIndex, """
                        invoke-virtual {p0}, $definingClass->getWindow()Landroid/view/Window;
                        move-result-object v$freeRegister
                        invoke-static {v$freeRegister}, $GENERAL_CLASS_DESCRIPTOR->disableDimBehind(Landroid/view/Window;)V
                        """
                )
            }
        }

    }
}

@Suppress("unused")
val overlayFilterPatch = resourcePatch(
    HIDE_OVERLAY_FILTER.title,
    HIDE_OVERLAY_FILTER.summary,
    use = false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        overlayFilterBytecodePatch,
    )

    execute {
        val styleFile = get("res/values/styles.xml")

        styleFile.writeText(
            styleFile.readText()
                .replace(
                    "ytOverlayBackgroundMedium\">@color/yt_black_pure_opacity60",
                    "ytOverlayBackgroundMedium\">@android:color/transparent"
                )
        )

        updatePatchStatus(HIDE_OVERLAY_FILTER)

    }
}
