package app.revanced.patches.music.general.autocaptions

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.DISABLE_AUTO_CAPTIONS
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.captions.baseAutoCaptionsPatch

@Suppress("unused")
val autoCaptionsPatch = bytecodePatch(
    DISABLE_AUTO_CAPTIONS.title,
    DISABLE_AUTO_CAPTIONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseAutoCaptionsPatch,
        settingsPatch
    )

    execute {
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_auto_captions",
            "false"
        )

        updatePatchStatus(DISABLE_AUTO_CAPTIONS)

    }
}