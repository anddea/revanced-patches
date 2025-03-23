package app.revanced.patches.music.utils.fix.parameter

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.MISC_PATH
import app.revanced.patches.music.utils.patch.PatchList.SPOOF_PLAYER_PARAMETER
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.music.video.information.videoInformationPatch
import app.revanced.patches.music.video.playerresponse.Hook
import app.revanced.patches.music.video.playerresponse.addPlayerResponseMethodHook
import app.revanced.patches.music.video.playerresponse.playerResponseMethodHookPatch
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.resolvable

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/SpoofPlayerParameterPatch;"

@Suppress("unused")
val spoofPlayerParameterPatch = bytecodePatch(
    SPOOF_PLAYER_PARAMETER.title,
    SPOOF_PLAYER_PARAMETER.summary
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        videoInformationPatch,
        playerResponseMethodHookPatch,
    )

    execute {

        addPlayerResponseMethodHook(
            Hook.PlayerParameter(
                "$EXTENSION_CLASS_DESCRIPTOR->spoofParameter(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
            ),
        )

        // region fix for subtitles position

        subtitleWindowFingerprint.methodOrThrow().addInstructions(
            0,
            """
                invoke-static {p1, p2, p3, p4, p5}, $EXTENSION_CLASS_DESCRIPTOR->fixSubtitleWindowPosition(IIIZZ)[I
                move-result-object v0
                const/4 v1, 0x0
                aget p1, v0, v1     # ap, anchor position
                const/4 v1, 0x1
                aget p2, v0, v1     # ah, horizontal anchor
                const/4 v1, 0x2
                aget p3, v0, v1     # av, vertical anchor
            """
        )

        // endregion

        // region fix for feature flags

        if (ageRestrictedPlaybackFeatureFlagFingerprint.resolvable()) {
            ageRestrictedPlaybackFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                AGE_RESTRICTED_PLAYBACK_FEATURE_FLAG,
                "$EXTENSION_CLASS_DESCRIPTOR->forceDisableAgeRestrictedPlaybackFeatureFlag(Z)Z"
            )
        }

        // endregion

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_spoof_player_parameter",
            "true"
        )

        updatePatchStatus(SPOOF_PLAYER_PARAMETER)

    }
}
