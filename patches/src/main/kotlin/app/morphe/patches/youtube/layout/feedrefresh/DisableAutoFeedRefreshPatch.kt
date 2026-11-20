/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3387
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.feedrefresh

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_AUTO_FEED_REFRESH
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/DisableAutoFeedRefreshPatch;"

@Suppress("unused")
val disableAutoFeedRefreshPatch = bytecodePatch(
    name = DISABLE_AUTO_FEED_REFRESH.title,
    description = DISABLE_AUTO_FEED_REFRESH.summary
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        addPreference(
            arrayOf("PREFERENCE_SCREEN: FEED", "SETTINGS: DISABLE_AUTO_FEED_REFRESH"),
            DISABLE_AUTO_FEED_REFRESH
        )

        FeedExpirationFingerprint.let {
            it.method.apply {
                // Later field first, so the earlier match index stays valid.
                listOf(5, 3).forEach { matchIndex ->
                    val match = it.instructionMatches[matchIndex]
                    val register = match.getInstruction<OneRegisterInstruction>().registerA

                    addInstructions(
                        match.index,
                        """
                            invoke-static/range { v$register .. v${register + 1} }, $EXTENSION_CLASS->getFeedExpirationTime(J)J
                            move-result-wide v$register
                        """
                    )
                }
            }
        }
    }
}
