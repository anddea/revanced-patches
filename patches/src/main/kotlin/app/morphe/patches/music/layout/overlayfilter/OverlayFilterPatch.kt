package app.morphe.patches.music.layout.overlayfilter

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.patch.PatchList.HIDE_OVERLAY_FILTER
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val overlayFilterBytecodePatch = bytecodePatch(
    description = "overlayFilterBytecodePatch"
) {
    dependsOn(sharedResourceIdPatch)

    execute {
        designBottomSheetDialogFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.instructionMatches.last().index - 1
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
