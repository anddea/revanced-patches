package app.revanced.patches.youtube.utils.fix.streamingdata

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.patches.shared.spoof.streamingdata.baseSpoofStreamingDataPatch
import app.revanced.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.revanced.patches.youtube.utils.patch.PatchList.SPOOF_STREAMING_DATA
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.findMethodOrThrow

val spoofStreamingDataPatch = baseSpoofStreamingDataPatch(
    {
        compatibleWith(COMPATIBLE_PACKAGE)

        dependsOn(
            baseSpoofUserAgentPatch(YOUTUBE_PACKAGE_NAME),
            settingsPatch
        )
    },
    {
        findMethodOrThrow("$PATCHES_PATH/PatchStatus;") {
            name == "SpoofStreamingDataAndroidOnlyDefaultBoolean"
        }.replaceInstruction(
            0,
            "const/4 v0, 0x1"
        )

        addPreference(
            arrayOf(
                "SETTINGS: SPOOF_STREAMING_DATA"
            ),
            SPOOF_STREAMING_DATA
        )

    }
)
