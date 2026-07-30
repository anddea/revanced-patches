/*
 * Portions of this file are adapted from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.music.utils.sponsorblock

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.EXTENSION_PATH
import app.morphe.patches.music.utils.patch.PatchList.SPONSORBLOCK
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.ACTIVITY_HOOK_TARGET_CLASS
import app.morphe.patches.music.utils.settings.ResourceUtils.PREFERENCE_CATEGORY_TAG_NAME
import app.morphe.patches.music.utils.settings.ResourceUtils.PREFERENCE_SCREEN_TAG_NAME
import app.morphe.patches.music.utils.settings.ResourceUtils.SETTINGS_HEADER_PATH
import app.morphe.patches.music.utils.settings.ResourceUtils.SWITCH_PREFERENCE_TAG_NAME
import app.morphe.patches.music.utils.settings.ResourceUtils.addPreferenceCategory
import app.morphe.patches.music.utils.settings.ResourceUtils.musicPackageName
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.music.video.information.videoIdHook
import app.morphe.patches.music.video.information.videoInformationPatch
import app.morphe.patches.music.video.information.videoTimeHook
import app.morphe.util.adoptChild
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/sponsorblock/SegmentPlaybackController;"
private const val SEGMENT_CATEGORY_PREFERENCE_TAG =
    "app.morphe.extension.music.settings.preference.SponsorBlockCategoryPreference"

private val sponsorBlockBytecodePatch = bytecodePatch(
    description = "sponsorBlockBytecodePatch"
) {
    dependsOn(
        sharedResourceIdPatch,
        videoInformationPatch
    )

    execute {

        /**
         * Hook the video time methods & Initialize the player controller
         */
        videoTimeHook(EXTENSION_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Responsible for seekbar in fullscreen
         */
        var rectangleFieldName =
            with(RectangleFieldInvalidatorFingerprint.method) {
                val invalidateIndex = indexOfInvalidateInstruction(this)
                val rectangleIndex =
                    indexOfFirstInstructionReversedOrThrow(invalidateIndex + 1) {
                        getReference<FieldReference>()?.type == "Landroid/graphics/Rect;"
                    }
                val rectangleReference =
                    getInstruction<ReferenceInstruction>(rectangleIndex).reference

                (rectangleReference as FieldReference).name
            }

        SeekbarOnDrawFingerprint.method.apply {
            // Initialize seekbar method
            addInstructions(
                0, """
                    move-object/from16 v0, p0
                    const-string v1, "$rectangleFieldName"
                    invoke-static {v0, v1}, $EXTENSION_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;Ljava/lang/String;)V
                    """
            )

            // Set seekbar thickness
            val roundIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "round"
            } + 1
            val roundRegister = getInstruction<OneRegisterInstruction>(roundIndex).registerA
            addInstruction(
                roundIndex + 1,
                "invoke-static {v$roundRegister}, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )

            // Draw segment
            val drawCircleIndex = indexOfFirstInstructionReversedOrThrow {
                getReference<MethodReference>()?.name == "drawCircle"
            }
            val drawCircleInstruction = getInstruction<FiveRegisterInstruction>(drawCircleIndex)
            addInstruction(
                drawCircleIndex,
                "invoke-static {v${drawCircleInstruction.registerC}, v${drawCircleInstruction.registerE}}, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
            )
        }


        /**
         * Responsible for seekbar in player
         */
        val rectField = MusicPlaybackControlsTimeBarOnMeasureFingerprint.method.run {
            val rectIndex = indexOfFirstInstructionReversedOrThrow(
                implementation!!.instructions.size - 1
            ) {
                opcode == Opcode.IGET_OBJECT &&
                        getReference<FieldReference>()?.type == "Landroid/graphics/Rect;"
            }
            getInstruction<ReferenceInstruction>(rectIndex).reference as FieldReference
        }

        MusicPlaybackControlsTimeBarDrawFingerprint.method.apply {
            // Inject after super.draw() so the markers render on every compact-player frame.
            val freeRegister = findFreeRegister(1)
            addInstructions(
                1,
                """
                    iget-object v$freeRegister, p0, $rectField
                    invoke-static {v$freeRegister}, $EXTENSION_CLASS_DESCRIPTOR->setSeekbarRectangle(Landroid/graphics/Rect;)V
                    invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->drawSegmentTimeBars(Landroid/graphics/Canvas;)V
                """
            )
        }

        /**
         * Set current video id
         */
        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")
    }
}

private const val SEGMENTS_CATEGORY_KEY = "sb_diff_segments"
private const val ABOUT_CATEGORY_KEY = "sb_about"

private val SPONSOR_BLOCK_CATEGORY = CategoryType.SPONSOR_BLOCK.value

@Suppress("unused")
val sponsorBlockPatch = resourcePatch(
    SPONSORBLOCK.title,
    SPONSORBLOCK.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    dependsOn(
        sponsorBlockBytecodePatch,
        settingsPatch,
    )

    execute {
        fun addSwitchPreference(
            category: String,
            key: String,
            defaultValue: String,
            dependencyKey: String,
            titleKey: String = "revanced_$key",
            summaryKey: String = "revanced_${key}_sum",
        ) {
            document(SETTINGS_HEADER_PATH).use { document ->
                val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter {
                        it.getAttribute("android:key")
                            .contains("revanced_preference_screen_$category")
                    }
                    .forEach {
                        it.adoptChild(SWITCH_PREFERENCE_TAG_NAME) {
                            setAttribute("android:title", "@string/$titleKey")
                            setAttribute("android:summary", "@string/$summaryKey")
                            setAttribute("android:key", key)
                            setAttribute("android:defaultValue", defaultValue)
                            if (dependencyKey != "") {
                                setAttribute("android:dependency", dependencyKey)
                            }
                        }
                    }
            }
        }

        fun addSwitchPreference(
            category: String,
            key: String,
            defaultValue: String
        ) = addSwitchPreference(category, key, defaultValue, "")

        fun addPreferenceWithIntent(
            category: String,
            key: String,
            dependencyKey: String
        ) {
            document(SETTINGS_HEADER_PATH).use { document ->
                val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter {
                        it.getAttribute("android:key")
                            .contains("revanced_preference_screen_$category")
                    }
                    .forEach {
                        it.adoptChild("Preference") {
                            setAttribute("android:title", "@string/revanced_$key")
                            setAttribute("android:summary", "@string/revanced_$key" + "_sum")
                            setAttribute("android:key", key)
                            setAttribute("android:dependency", dependencyKey)
                            this.adoptChild("intent") {
                                setAttribute("android:targetPackage", musicPackageName)
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

        fun addPreferenceCategoryUnderPreferenceScreen(
            preferenceScreenKey: String,
            category: String,
            titleKey: String = "revanced_$category",
        ) {
            document(SETTINGS_HEADER_PATH).use { document ->
                val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter { it.getAttribute("android:key").contains(preferenceScreenKey) }
                    .forEach {
                        it.adoptChild(PREFERENCE_CATEGORY_TAG_NAME) {
                            setAttribute("android:title", "@string/$titleKey")
                            setAttribute("android:key", category)
                        }
                    }
            }
        }

        fun addSegmentsPreference(
            key: String,
            categoryKey: String,
            dependencyKey: String
        ) {
            document(SETTINGS_HEADER_PATH).use { document ->
                val tags = document.getElementsByTagName(PREFERENCE_CATEGORY_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter { it.getAttribute("android:key") == SEGMENTS_CATEGORY_KEY }
                    .forEach {
                        it.adoptChild(SEGMENT_CATEGORY_PREFERENCE_TAG) {
                            setAttribute("android:title", "@string/revanced_$key")
                            setAttribute("android:summary", "@string/revanced_$key" + "_sum")
                            setAttribute("android:key", "sb_${categoryKey}_color")
                            setAttribute("android:selectable", "true")
                            setAttribute("android:dependency", dependencyKey)
                        }
                    }
            }
        }

        fun addAboutPreference(
            key: String,
            data: String
        ) {
            document(SETTINGS_HEADER_PATH).use { document ->
                val tags = document.getElementsByTagName(PREFERENCE_CATEGORY_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter { it.getAttribute("android:key") == ABOUT_CATEGORY_KEY }
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

        addPreferenceCategory(SPONSOR_BLOCK_CATEGORY)

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
            "true",
            "sb_enabled",
            titleKey = "revanced_ryd_toast_on_connection_error_title",
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
            "sb_segments_sponsor",
            "sponsor",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_selfpromo",
            "selfpromo",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_interaction",
            "interaction",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_intro",
            "intro",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_outro",
            "outro",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_preview",
            "preview",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_hook",
            "hook",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_filler",
            "filler",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_nomusic",
            "music_offtopic",
            "sb_enabled"
        )

        addPreferenceCategoryUnderPreferenceScreen(
            CategoryType.SPONSOR_BLOCK.value,
            ABOUT_CATEGORY_KEY,
            titleKey = "morphe_music_crossfade_about_title",
        )

        addAboutPreference(
            "sb_about_api",
            "https://sponsor.ajay.app"
        )

        updatePatchStatus(SPONSORBLOCK)

    }
}
