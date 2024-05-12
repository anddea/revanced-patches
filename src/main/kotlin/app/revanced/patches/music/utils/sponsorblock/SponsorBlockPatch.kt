package app.revanced.patches.music.utils.sponsorblock

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils
import app.revanced.patches.music.utils.settings.ResourceUtils.ACTIVITY_HOOK_TARGET_CLASS
import app.revanced.patches.music.utils.settings.ResourceUtils.PREFERENCE_CATEGORY_TAG_NAME
import app.revanced.patches.music.utils.settings.ResourceUtils.PREFERENCE_SCREEN_TAG_NAME
import app.revanced.patches.music.utils.settings.ResourceUtils.SETTINGS_HEADER_PATH
import app.revanced.patches.music.utils.settings.ResourceUtils.SWITCH_PREFERENCE_TAG_NAME
import app.revanced.patches.music.utils.settings.ResourceUtils.addPreferenceCategory
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.adoptChild
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element

@Suppress("DEPRECATION", "SameParameterValue", "unused")
object SponsorBlockPatch : BaseResourcePatch(
    name = "SponsorBlock",
    description = "Adds options to enable and configure SponsorBlock, which can skip undesired video segments such as non-music sections.",
    dependencies = setOf(
        SettingsPatch::class,
        SponsorBlockBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val SEGMENTS_CATEGORY_KEY = "sb_diff_segments"
    private const val ABOUT_CATEGORY_KEY = "sb_about"

    private var SPONSOR_BLOCK_CATEGORY = CategoryType.SPONSOR_BLOCK.value
    lateinit var context: ResourceContext

    override fun execute(context: ResourceContext) {
        this.context = context

        context.addPreferenceCategory(SPONSOR_BLOCK_CATEGORY)

        addSwitchPreference(
            SPONSOR_BLOCK_CATEGORY,
            "sb_enabled",
            "true"
        )
        addSwitchPreference(
            SPONSOR_BLOCK_CATEGORY,
            "sb_toast_on_skip",
            "true",
            "sb_enabled"
        )
        addSwitchPreference(
            SPONSOR_BLOCK_CATEGORY,
            "sb_toast_on_connection_error",
            "false",
            "sb_enabled"
        )
        addPreferenceWithIntent(
            SPONSOR_BLOCK_CATEGORY,
            "sb_api_url",
            "sb_enabled"
        )

        addPreferenceCategoryUnderPreferenceScreen(
            SPONSOR_BLOCK_CATEGORY,
            SEGMENTS_CATEGORY_KEY
        )

        addSegmentsPreference(
            SEGMENTS_CATEGORY_KEY,
            "sb_segments_sponsor",
            "sb_enabled"
        )
        addSegmentsPreference(
            SEGMENTS_CATEGORY_KEY,
            "sb_segments_selfpromo",
            "sb_enabled"
        )
        addSegmentsPreference(
            SEGMENTS_CATEGORY_KEY,
            "sb_segments_interaction",
            "sb_enabled"
        )
        addSegmentsPreference(
            SEGMENTS_CATEGORY_KEY,
            "sb_segments_intro",
            "sb_enabled"
        )
        addSegmentsPreference(
            SEGMENTS_CATEGORY_KEY,
            "sb_segments_outro",
            "sb_enabled"
        )
        addSegmentsPreference(
            SEGMENTS_CATEGORY_KEY,
            "sb_segments_preview",
            "sb_enabled"
        )
        addSegmentsPreference(
            SEGMENTS_CATEGORY_KEY,
            "sb_segments_filler",
            "sb_enabled"
        )
        addSegmentsPreference(
            SEGMENTS_CATEGORY_KEY,
            "sb_segments_nomusic",
            "sb_enabled"
        )

        addPreferenceCategoryUnderPreferenceScreen(
            CategoryType.SPONSOR_BLOCK.value,
            ABOUT_CATEGORY_KEY
        )

        addAboutPreference(
            ABOUT_CATEGORY_KEY,
            "sb_about_api",
            "https://sponsor.ajay.app"
        )

        context[SETTINGS_HEADER_PATH].apply {
            writeText(
                readText()
                    .replace(
                        "\"sb_segments_nomusic",
                        "\"sb_segments_music_offtopic"
                    )
            )
        }

    }

    private fun addSwitchPreference(
        category: String,
        key: String,
        defaultValue: String
    ) = addSwitchPreference(category, key, defaultValue, "")

    private fun addSwitchPreference(
        category: String,
        key: String,
        defaultValue: String,
        dependencyKey: String
    ) {
        context.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains("revanced_preference_screen_$category") }
                .forEach {
                    it.adoptChild(SWITCH_PREFERENCE_TAG_NAME) {
                        setAttribute("android:title", "@string/revanced_$key")
                        setAttribute("android:summary", "@string/revanced_$key" + "_sum")
                        setAttribute("android:key", key)
                        setAttribute("android:defaultValue", defaultValue)
                        if (dependencyKey != "") {
                            setAttribute("android:dependency", dependencyKey)
                        }
                    }
                }
        }
    }

    private fun addPreferenceWithIntent(
        category: String,
        key: String,
        dependencyKey: String
    ) {
        context.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains("revanced_preference_screen_$category") }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/revanced_$key")
                        setAttribute("android:summary", "@string/revanced_$key" + "_sum")
                        setAttribute("android:key", key)
                        setAttribute("android:dependency", dependencyKey)
                        this.adoptChild("intent") {
                            setAttribute("android:targetPackage", ResourceUtils.musicPackageName)
                            setAttribute("android:data", key)
                            setAttribute(
                                "android:targetClass",
                                ACTIVITY_HOOK_TARGET_CLASS
                            )
                        }
                    }
                }
        }
    }

    private fun addPreferenceCategoryUnderPreferenceScreen(
        preferenceScreenKey: String,
        category: String
    ) {
        context.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(preferenceScreenKey) }
                .forEach {
                    it.adoptChild(PREFERENCE_CATEGORY_TAG_NAME) {
                        setAttribute("android:title", "@string/revanced_$category")
                        setAttribute("android:key", category)
                    }
                }
        }
    }

    private fun addSegmentsPreference(
        preferenceCategoryKey: String,
        key: String,
        dependencyKey: String
    ) {
        context.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_CATEGORY_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(preferenceCategoryKey) }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/revanced_$key")
                        setAttribute("android:summary", "@string/revanced_$key" + "_sum")
                        setAttribute("android:key", key)
                        setAttribute("android:dependency", dependencyKey)
                        this.adoptChild("intent") {
                            setAttribute("android:targetPackage", ResourceUtils.musicPackageName)
                            setAttribute("android:data", key)
                            setAttribute(
                                "android:targetClass",
                                ACTIVITY_HOOK_TARGET_CLASS
                            )
                        }
                    }
                }
        }
    }

    private fun addAboutPreference(
        preferenceCategoryKey: String,
        key: String,
        data: String
    ) {
        context.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_CATEGORY_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(preferenceCategoryKey) }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/revanced_$key")
                        setAttribute("android:summary", "@string/revanced_$key" + "_sum")
                        setAttribute("android:key", key)
                        this.adoptChild("intent") {
                            setAttribute("android:action", "android.intent.action.VIEW")
                            setAttribute("android:data", data)
                        }
                    }
                }
        }
    }
}