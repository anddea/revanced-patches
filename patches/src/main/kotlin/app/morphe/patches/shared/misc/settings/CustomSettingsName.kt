/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2691
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.shared.misc.settings

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patches.shared.misc.settings.preference.ListPreference
import app.morphe.patches.shared.misc.settingsmenu.PreferenceSetTitleFingerprint
import app.morphe.patches.shared.settingmenu.PreferenceGroupFindPreferenceFingerprint

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/shared/patches/SettingsNamePatch;"

/** Key of the setting that stores a preset value or a name entered by the user. */
internal const val SETTINGS_NAME_PREFERENCE_KEY = "morphe_settings_name"

/** Key used by the YouTube settings screen for the RVX entry. */
internal const val YOUTUBE_SETTINGS_ENTRY_KEY = "revanced_settings_key"

/** Key used by the YouTube Music settings screen for the RVX entry. */
internal const val MUSIC_SETTINGS_ENTRY_KEY = "revanced_settings"

internal fun customSettingsNamePreference() = ListPreference(
    key = SETTINGS_NAME_PREFERENCE_KEY,
    tag = "app.morphe.extension.shared.settings.preference.SettingsNamePreference"
)

/**
 * Renames the entry keyed by [preferenceKey], and does nothing if the user
 * set no name of their own. All three registers must be free at the insertion point.
 *
 * @param getPreferenceScreen Instructions that leave the root PreferenceScreen in [screenRegister].
 */
context(patchContext: BytecodePatchContext)
internal fun customSettingsNameInstructions(
    preferenceKey: String,
    getPreferenceScreen: String,
    screenRegister: Int,
    preferenceRegister: Int,
    nameRegister: Int
) = """
    invoke-static { }, $EXTENSION_CLASS->getCustomSettingsName()Ljava/lang/String;
    move-result-object v$nameRegister
    if-eqz v$nameRegister, :morphe_settings_name_exit

    $getPreferenceScreen
    if-eqz v$screenRegister, :morphe_settings_name_exit

    const-string v$preferenceRegister, "$preferenceKey"
    invoke-virtual { v$screenRegister, v$preferenceRegister }, ${PreferenceGroupFindPreferenceFingerprint.method}
    move-result-object v$preferenceRegister
    if-eqz v$preferenceRegister, :morphe_settings_name_exit

    invoke-virtual { v$preferenceRegister, v$nameRegister }, ${PreferenceSetTitleFingerprint.method}

    :morphe_settings_name_exit
    nop
"""
