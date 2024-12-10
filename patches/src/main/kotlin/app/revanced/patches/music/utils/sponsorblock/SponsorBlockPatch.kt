package app.revanced.patches.music.utils.sponsorblock

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.music.utils.patch.PatchList.SPONSORBLOCK
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.ACTIVITY_HOOK_TARGET_CLASS
import app.revanced.patches.music.utils.settings.ResourceUtils.PREFERENCE_CATEGORY_TAG_NAME
import app.revanced.patches.music.utils.settings.ResourceUtils.PREFERENCE_SCREEN_TAG_NAME
import app.revanced.patches.music.utils.settings.ResourceUtils.SETTINGS_HEADER_PATH
import app.revanced.patches.music.utils.settings.ResourceUtils.SWITCH_PREFERENCE_TAG_NAME
import app.revanced.patches.music.utils.settings.ResourceUtils.addPreferenceCategory
import app.revanced.patches.music.utils.settings.ResourceUtils.musicPackageName
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.music.video.information.videoIdHook
import app.revanced.patches.music.video.information.videoInformationPatch
import app.revanced.patches.music.video.information.videoTimeHook
import app.revanced.util.adoptChild
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/sponsorblock/SegmentPlaybackController;"

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
            with(rectangleFieldInvalidatorFingerprint.methodOrThrow(seekBarConstructorFingerprint)) {
                val invalidateIndex = indexOfInvalidateInstruction(this)
                val rectangleIndex =
                    indexOfFirstInstructionReversedOrThrow(invalidateIndex + 1) {
                        getReference<FieldReference>()?.type == "Landroid/graphics/Rect;"
                    }
                val rectangleReference =
                    getInstruction<ReferenceInstruction>(rectangleIndex).reference

                (rectangleReference as FieldReference).name
            }

        seekbarOnDrawFingerprint.methodOrThrow(seekBarConstructorFingerprint).apply {
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
        rectangleFieldName =
            musicPlaybackControlsTimeBarOnMeasureFingerprint.matchOrThrow().let {
                with(it.method) {
                    val rectangleIndex = it.patternMatch!!.startIndex
                    val rectangleReference =
                        getInstruction<ReferenceInstruction>(rectangleIndex).reference
                    (rectangleReference as FieldReference).name
                }
            }

        musicPlaybackControlsTimeBarDrawFingerprint.methodOrThrow().apply {
            // Initialize seekbar method
            addInstructions(
                1, """
                    move-object/from16 v0, p0
                    const-string v1, "$rectangleFieldName"
                    invoke-static {v0, v1}, $EXTENSION_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;Ljava/lang/String;)V
                    """
            )

            // Draw segment
            val drawCircleIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
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
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sponsorBlockBytecodePatch,
        settingsPatch,
    )

    execute {
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

        get(SETTINGS_HEADER_PATH).apply {
            writeText(
                readText()
                    .replace(
                        "\"sb_segments_nomusic",
                        "\"sb_segments_music_offtopic"
                    )
            )
        }

        updatePatchStatus(SPONSORBLOCK)

    }
}

private fun ResourcePatchContext.addSwitchPreference(
    category: String,
    key: String,
    defaultValue: String
) = addSwitchPreference(category, key, defaultValue, "")

private fun ResourcePatchContext.addSwitchPreference(
    category: String,
    key: String,
    defaultValue: String,
    dependencyKey: String
) {
    document(SETTINGS_HEADER_PATH).use { document ->
        val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
        List(tags.length) { tags.item(it) as Element }
            .filter {
                it.getAttribute("android:key").contains("revanced_preference_screen_$category")
            }
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

private fun ResourcePatchContext.addPreferenceWithIntent(
    category: String,
    key: String,
    dependencyKey: String
) {
    document(SETTINGS_HEADER_PATH).use { document ->
        val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
        List(tags.length) { tags.item(it) as Element }
            .filter {
                it.getAttribute("android:key").contains("revanced_preference_screen_$category")
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

private fun ResourcePatchContext.addPreferenceCategoryUnderPreferenceScreen(
    preferenceScreenKey: String,
    category: String
) {
    document(SETTINGS_HEADER_PATH).use { document ->
        val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
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

private fun ResourcePatchContext.addSegmentsPreference(
    preferenceCategoryKey: String,
    key: String,
    dependencyKey: String
) {
    document(SETTINGS_HEADER_PATH).use { document ->
        val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
        List(tags.length) { tags.item(it) as Element }
            .filter { it.getAttribute("android:key").contains(preferenceCategoryKey) }
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

private fun ResourcePatchContext.addAboutPreference(
    preferenceCategoryKey: String,
    key: String,
    data: String
) {
    document(SETTINGS_HEADER_PATH).use { document ->
        val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
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

