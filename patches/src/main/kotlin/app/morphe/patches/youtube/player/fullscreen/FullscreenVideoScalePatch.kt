/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2616
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.player.fullscreen

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.player.overlaybuttons.overlayButtonsPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.FULLSCREEN_VIDEO_SCALE
import app.morphe.patches.youtube.utils.playercontrols.injectControl
import app.morphe.patches.youtube.utils.playercontrols.playerControlsPatch
import app.morphe.patches.youtube.utils.playertype.PlayerTypeEnumFingerprint
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.AccessFlags

private const val EXTENSION_CLASS_VIDEO_SCALE =
    "Lapp/morphe/extension/youtube/patches/FullscreenVideoScalePatch;"
private const val EXTENSION_BUTTON =
    "Lapp/morphe/extension/youtube/videoplayer/FullscreenVideoScaleButton;"

private val fullscreenVideoScaleResourcePatch = resourcePatch {
    dependsOn(
        overlayButtonsPatch,
        settingsPatch,
        playerControlsPatch,
    )

    execute {
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "SETTINGS: FULLSCREEN_VIDEO_SCALE",
            ),
            FULLSCREEN_VIDEO_SCALE,
        )
    }
}

@Suppress("unused")
val fullscreenVideoScalePatch = bytecodePatch(
    name = FULLSCREEN_VIDEO_SCALE.title,
    description = FULLSCREEN_VIDEO_SCALE.summary,
) {
    dependsOn(
        sharedExtensionPatch,
        playerTypeHookPatch,
        playerControlsPatch,
        fullscreenVideoScaleResourcePatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        injectControl(EXTENSION_BUTTON, false)

        Fingerprint(
            classFingerprint = YouTubePlayerOverlaysLayoutConstructorFingerprint,
            accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
            returnType = "V",
            parameters = listOf(PlayerTypeEnumFingerprint.originalClassDef.type),
        ).method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION_CLASS_VIDEO_SCALE->" +
                    "attachPlayerOverlay(Landroid/view/View;)V",
        )

        YouTubePlayerOverlaysLayoutConstructorFingerprint.matchAll().forEach {
            it.method.addInstruction(
                it.instructionMatches.first().index,
                "invoke-static { p0 }, $EXTENSION_CLASS_VIDEO_SCALE->" +
                        "attachPlayerOverlay(Landroid/view/View;)V",
            )
        }

        YouTubePlayerViewOnLayoutFingerprint.let {
            it.method.addInstructionsAtControlFlowLabel(
                it.instructionMatches.first().index,
                "invoke-static { p0 }, $EXTENSION_CLASS_VIDEO_SCALE->" +
                        "onPlayerViewLayout(Landroid/view/View;)V",
            )
        }
    }
}
