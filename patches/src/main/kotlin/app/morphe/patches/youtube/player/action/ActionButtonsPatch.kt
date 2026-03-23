package app.morphe.patches.youtube.player.action

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.youtube.shared.WatchNextResponseParserFingerprint
import app.morphe.patches.youtube.utils.auth.authHookPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.componentlist.hookElementList
import app.morphe.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.fix.hype.hypeButtonIconPatch
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_ACTION_BUTTONS
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.morphe.patches.youtube.video.videoid.videoIdPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ActionButtonsFilter;"
private const val ACTION_BUTTONS_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/ActionButtonsPatch;"

@Suppress("unused")
val actionButtonsPatch = bytecodePatch(
    HIDE_ACTION_BUTTONS.title,
    HIDE_ACTION_BUTTONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        lazilyConvertedElementHookPatch,
        fixProtoLibraryPatch,
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
