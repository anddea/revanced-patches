package app.revanced.patches.music.utils.returnyoutubedislike

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.music.utils.patch.PatchList.RETURN_YOUTUBE_DISLIKE
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.PREFERENCE_CATEGORY_TAG_NAME
import app.revanced.patches.music.utils.settings.ResourceUtils.SETTINGS_HEADER_PATH
import app.revanced.patches.music.utils.settings.ResourceUtils.addPreferenceCategoryUnderPreferenceScreen
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.music.video.information.videoIdHook
import app.revanced.patches.music.video.information.videoInformationPatch
import app.revanced.patches.shared.dislikeFingerprint
import app.revanced.patches.shared.likeFingerprint
import app.revanced.patches.shared.removeLikeFingerprint
import app.revanced.util.adoptChild
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/ReturnYouTubeDislikePatch;"

private val returnYouTubeDislikeBytecodePatch = bytecodePatch(
    description = "returnYouTubeDislikeBytecodePatch"
) {
    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        videoInformationPatch
    )

    execute {

        mapOf(
            likeFingerprint to Vote.LIKE,
            dislikeFingerprint to Vote.DISLIKE,
            removeLikeFingerprint to Vote.REMOVE_LIKE,
        ).forEach { (fingerprint, vote) ->
            fingerprint.methodOrThrow().addInstructions(
                0,
                """
                    const/4 v0, ${vote.value}
                    invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->sendVote(I)V
                    """,
            )
        }


        textComponentFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_STATIC
                        && (this as ReferenceInstruction).reference.toString()
                    .endsWith("Ljava/lang/CharSequence;")
            } + 2
            val insertRegister =
                getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->onSpannedCreated(Landroid/text/Spanned;)Landroid/text/Spanned;
                    move-result-object v$insertRegister
                    """
            )
        }

        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")
    }
}

enum class Vote(val value: Int) {
    LIKE(1),
    DISLIKE(-1),
    REMOVE_LIKE(0),
}

private const val ABOUT_CATEGORY_KEY = "revanced_ryd_about"
private const val RYD_ATTRIBUTION_KEY = "revanced_ryd_attribution"

@Suppress("unused")
val returnYouTubeDislikePatch = resourcePatch(
    RETURN_YOUTUBE_DISLIKE.title,
    RETURN_YOUTUBE_DISLIKE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        returnYouTubeDislikeBytecodePatch,
        settingsPatch,
    )

    execute {
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "revanced_ryd_enabled",
            "true"
        )
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "revanced_ryd_dislike_percentage",
            "false",
            "revanced_ryd_enabled"
        )
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "revanced_ryd_compact_layout",
            "false",
            "revanced_ryd_enabled"
        )
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "revanced_ryd_estimated_like",
            "false",
            "revanced_ryd_enabled"
        )
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "revanced_ryd_toast_on_connection_error",
            "false",
            "revanced_ryd_enabled"
        )

        addPreferenceCategoryUnderPreferenceScreen(
            CategoryType.RETURN_YOUTUBE_DISLIKE.value,
            ABOUT_CATEGORY_KEY
        )

        document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_CATEGORY_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(ABOUT_CATEGORY_KEY) }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/$RYD_ATTRIBUTION_KEY" + "_title")
                        setAttribute("android:summary", "@string/$RYD_ATTRIBUTION_KEY" + "_summary")
                        setAttribute("android:key", RYD_ATTRIBUTION_KEY)
                        this.adoptChild("intent") {
                            setAttribute("android:action", "android.intent.action.VIEW")
                            setAttribute("android:data", "https://returnyoutubedislike.com")
                        }
                    }
                }
        }

        updatePatchStatus(RETURN_YOUTUBE_DISLIKE)

    }
}

