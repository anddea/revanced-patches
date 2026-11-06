/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3592
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.interaction.copytext

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.textcomponent.copytext.hookLithoSpannableString
import app.morphe.patches.shared.textcomponent.copytext.lithoSpannableStringPatch
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.patch.PatchList.COPY_TEXT
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.video.information.videoInformationPatch

private const val EXTENSION_CLASS = "Lapp/morphe/extension/youtube/patches/CopyTextPatch;"

@Suppress("unused")
val copyTextPatch = bytecodePatch(
    COPY_TEXT.title,
    COPY_TEXT.summary
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        lithoSpannableStringPatch,
        videoInformationPatch
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        addPreference(
            arrayOf("PREFERENCE_SCREEN: GENERAL", "SETTINGS: COPY_TEXT"),
            COPY_TEXT,
        )

        // Verify stubbed classes are not obfuscated.
        classDefBy("Lcom/facebook/litho/TextContent;")

        // The shown texts are recognized by the path of their component,
        // and the title can be auto-translated, so it is taken from the text that is drawn.
        hookLithoSpannableString(EXTENSION_CLASS)

        // The texts are drawn by Litho and have no view of their own,
        // so the host that draws them is found by its text when touched.
        ComponentHostDispatchTouchEventFingerprint.method.addInstruction(
            0,
            "invoke-static { p0, p1 }, $EXTENSION_CLASS->" +
                    "onComponentHostTouch(Lcom/facebook/litho/ComponentHost;Landroid/view/MotionEvent;)V"
        )
    }
}
