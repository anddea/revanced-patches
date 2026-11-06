/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4222
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.player.livechat

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.litho.context.conversionContextPatch
import app.morphe.patches.shared.litho.relayout.lithoRelayoutPatch
import app.morphe.patches.shared.textcomponent.hookTextComponent
import app.morphe.patches.shared.textcomponent.textComponentPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.TRANSLATE_LIVE_CHAT
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/LiveChatTranslatePatch;"

@Suppress("unused")
val liveChatTranslatePatch = bytecodePatch(
    name = "Translate live chat",
    description = "Adds an option to translate live chat and live chat replay messages.",
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        conversionContextPatch,
        textComponentPatch,
        // Shows the translation in the chat rows that are already on screen.
        lithoRelayoutPatch,
    )
    execute {
        addPreference(
            arrayOf("PREFERENCE_SCREEN: PLAYER", "SETTINGS: TRANSLATE_LIVE_CHAT"),
            TRANSLATE_LIVE_CHAT,
        )
        // Chat rows are Litho texts, which are replaced when created and when reused.
        hookTextComponent(EXTENSION_CLASS)
    }
}
