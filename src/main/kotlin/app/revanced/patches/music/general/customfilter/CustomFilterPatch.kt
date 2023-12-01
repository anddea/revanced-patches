package app.revanced.patches.music.general.customfilter

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.litho.LithoFilterPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_COMPONENTS_PATH

@Patch(
    name = "Enable custom filter",
    description = "Enables custom filter to hide layout components.",
    dependencies = [
        LithoFilterPatch::class,
        SettingsPatch::class
    ],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object CustomFilterPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_custom_filter",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_custom_filter_strings",
            "revanced_custom_filter"
        )

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

    }

    private const val FILTER_CLASS_DESCRIPTOR =
        "$MUSIC_COMPONENTS_PATH/CustomFilter;"
}
