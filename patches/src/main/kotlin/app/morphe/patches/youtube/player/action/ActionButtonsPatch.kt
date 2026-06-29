/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.player.action

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.literal
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.youtube.general.updates.cronetHeaderFingerprint
import app.morphe.patches.youtube.shared.WatchNextResponseParserFingerprint
import app.morphe.patches.youtube.utils.auth.authHookPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.componentlist.hookElementList
import app.morphe.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.fix.hype.hypeButtonIconPatch
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_ACTION_BUTTONS
import app.morphe.patches.youtube.utils.playservice.is_20_21_or_greater
import app.morphe.patches.youtube.utils.request.buildRequestPatch
import app.morphe.patches.youtube.utils.request.hookBuildRequest
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.morphe.patches.youtube.video.videoid.videoIdPatch
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ActionButtonsFilter;"
private const val ACTION_BUTTONS_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/ActionButtonsPatch;"

private object ModernRelateVideoOverlayFingerprint : Fingerprint(
    filters = listOf(
        literal(45614162L)
    )
)

private object RelateVideoOverlayLayoutParamFingerprint : Fingerprint(
    filters = listOf(
        literal(45661108L)
    )
)

internal val restoreOldVideoActionBarPatch = bytecodePatch(
    description = "restoreOldVideoActionBarPatch"
) {
    dependsOn(
        settingsPatch,
        buildRequestPatch,
        fixProtoLibraryPatch,
    )

    execute {
        if (is_20_21_or_greater) {
            addPreference(
                arrayOf(
                    "PREFERENCE_SCREEN: PLAYER",
                    "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                    "SETTINGS: RESTORE_OLD_VIDEO_ACTION_BAR"
                )
            )

            hookBuildRequest("$GENERAL_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Ljava/util/Map;)V")

            cronetHeaderFingerprint.matchOrThrow().let {
                it.method.apply {
                    val index = it.stringMatches.first().index

                    addInstructions(
                        index,
                        """
                        invoke-static {p1, p2}, $GENERAL_CLASS_DESCRIPTOR->fixVideoActionBar(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p2
                    """
                    )
                }
            }

            listOf(
                ModernRelateVideoOverlayFingerprint,
                RelateVideoOverlayLayoutParamFingerprint
            ).forEach { fingerprint ->
                fingerprint.let {
                    it.method.insertLiteralOverride(
                        it.instructionMatches.first().index,
                        "$GENERAL_CLASS_DESCRIPTOR->fixRelatedVideoOverlay(Z)Z"
                    )
                }
            }
        }
    }
}

@Suppress("unused")
val actionButtonsPatch = bytecodePatch(
    HIDE_ACTION_BUTTONS.title,
    HIDE_ACTION_BUTTONS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        lazilyConvertedElementHookPatch,
        fixProtoLibraryPatch,
        restoreOldVideoActionBarPatch,
        videoInformationPatch,
        videoIdPatch,
        authHookPatch,
        hypeButtonIconPatch,
    )

    execute {
        addLithoFilter(FILTER_CLASS_DESCRIPTOR)
        hookElementList("$FILTER_CLASS_DESCRIPTOR->onLazilyConvertedElementLoaded")

        // region patch for hide action buttons by index

        hookPlayerResponseVideoId("$ACTION_BUTTONS_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Z)V")
        hookElementList("$ACTION_BUTTONS_CLASS_DESCRIPTOR->hideActionButtonByIndex")

        // endregion

        WatchNextResponseParserFingerprint.let {
            it.clearMatch()
            it.method.apply {
                val index = it.instructionMatches[5].index
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $FILTER_CLASS_DESCRIPTOR->" +
                        "onSingleColumnWatchNextResultsLoaded(Lcom/google/protobuf/MessageLite;)V"
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: HIDE_ACTION_BUTTONS"
            ),
            HIDE_ACTION_BUTTONS
        )

        // endregion

    }
}
