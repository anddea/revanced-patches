/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.splashanimation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.playservice.is_20_05_or_greater
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val THEME_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/youtube/patches/theme/ThemePatch;"

internal val splashScreenAnimationBytecodePatch = bytecodePatch(
    description = "splashScreenAnimationBytecodePatch",
) {
    dependsOn(settingsPatch)

    execute {
        if (!is_20_05_or_greater) return@execute

        // Lottie splash screen exists in earlier versions, but it may not be always on.
        SplashScreenStyleFingerprint.let {
            it.method.insertLiteralOverride(
                it.instructionMatches.first().index,
                "$THEME_CLASS_DESCRIPTOR->getLoadingScreenType(I)I"
            )
        }

        ShowSplashScreenFingerprint.let {
            it.method.apply {
                val lastIndex = it.instructionMatches.last().index
                val lastInstruction = getInstruction<TwoRegisterInstruction>(lastIndex)
                val lastRegisterA = lastInstruction.registerA
                val lastRegisterB = lastInstruction.registerB

                addInstructions(
                    lastIndex,
                    """
                        invoke-static { v$lastRegisterA, v$lastRegisterB }, $THEME_CLASS_DESCRIPTOR->showSplashScreen(II)I
                        move-result v$lastRegisterA
                    """
                )

                val firstIndex = it.instructionMatches[1].index
                val firstRegister = getInstruction<OneRegisterInstruction>(
                    firstIndex
                ).registerA

                addInstructions(
                    firstIndex + 1,
                    """
                        invoke-static { v$firstRegister }, $THEME_CLASS_DESCRIPTOR->showSplashScreen(Z)Z
                        move-result v$firstRegister
                    """
                )
            }
        }
    }
}
