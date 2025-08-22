package app.revanced.patches.youtube.general.spoofappversion

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.spoof.appversion.baseSpoofAppVersionPatch
import app.revanced.patches.youtube.utils.CAIRO_FRAGMENT_FEATURE_FLAG
import app.revanced.patches.youtube.utils.cairoFragmentConfigFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.indexOfGetDrawableInstruction
import app.revanced.patches.youtube.utils.patch.PatchList.SPOOF_APP_VERSION
import app.revanced.patches.youtube.utils.playservice.is_19_26_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_28_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_29_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_34_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.settingsFragment
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.cairoFragmentDisabled
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.settingsFragmentSyntheticFingerprint
import app.revanced.patches.youtube.utils.toolBarButtonFingerprint
import app.revanced.util.Utils.printWarn
import app.revanced.util.appendAppVersion
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val spoofAppVersionBytecodePatch = bytecodePatch(
    description = "spoofAppVersionBytecodePatch"
) {

    dependsOn(
        settingsPatch,
        versionCheckPatch
    )

    execute {
        if (!is_19_26_or_greater) {
            return@execute
        }

        findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
            name == "SpoofAppVersion"
        }.returnEarly(true)

        /**
         * When spoofing the app version to YouTube 19.20.xx or earlier via Spoof app version on YouTube 19.23.xx+, the Library tab will crash.
         * As a temporary workaround, do not set an image in the toolbar when the enum name is UNKNOWN.
         */
        toolBarButtonFingerprint.methodOrThrow().apply {
            val getDrawableIndex = indexOfGetDrawableInstruction(this)
            val enumOrdinalIndex = indexOfFirstInstructionReversedOrThrow(getDrawableIndex) {
                opcode == Opcode.INVOKE_INTERFACE &&
                        getReference<MethodReference>()?.returnType == "I"
            }
            val insertIndex = enumOrdinalIndex + 2
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA
            val jumpIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setImageDrawable"
            } + 1

            addInstructionsWithLabels(
                insertIndex, """
                    if-eqz v$insertRegister, :ignore
                    """, ExternalLabel("ignore", getInstruction(jumpIndex))
            )
        }

        /**
         * RVX does not use CairoFragment, and uses a different method to restore the 'Playback' setting.
         * If the app version is spoofed to 19.30 or earlier, the 'Playback' setting will be broken.
         * Add a setting to fix this.
         */
        if (is_19_34_or_greater && cairoFragmentDisabled) {
            cairoFragmentConfigFingerprint.injectLiteralInstructionBooleanCall(
                CAIRO_FRAGMENT_FEATURE_FLAG,
                "$GENERAL_CLASS_DESCRIPTOR->disableCairoFragment(Z)Z"
            )

            settingsFragmentSyntheticFingerprint.methodOrThrow().apply {
                val index = indexOfFirstLiteralInstructionOrThrow(settingsFragment)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1, """
                        invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->useLegacyFragment(I)I
                        move-result v$register
                        """
                )
            }
        }
    }

}

@Suppress("unused")
val spoofAppVersionPatch = resourcePatch(
    SPOOF_APP_VERSION.title,
    SPOOF_APP_VERSION.summary,
) {
    compatibleWith(
        YOUTUBE_PACKAGE_NAME(
            "19.43.41",
            "19.44.39",
            "19.47.53",
        ),
    )

    dependsOn(
        baseSpoofAppVersionPatch("$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"),
        spoofAppVersionBytecodePatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        if (!is_19_26_or_greater) {
            printWarn("\"${SPOOF_APP_VERSION.title}\" is not supported in this version. Use YouTube 19.43.41 or later.")
            return@execute
        }

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL",
            "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
            "SETTINGS: SPOOF_APP_VERSION"
        )

        if (is_19_34_or_greater && cairoFragmentDisabled) {
            settingArray += "SETTINGS: FIX_SPOOF_APP_VERSION_SIDE_EFFECT"
        }

        addPreference(
            settingArray,
            SPOOF_APP_VERSION
        )

        appendAppVersion("19.01.34")

        if (is_19_28_or_greater) {
            appendAppVersion("19.26.42")
        } else {
            return@execute
        }

        if (is_19_29_or_greater) {
            appendAppVersion("19.28.42")
        } else {
            return@execute
        }

        if (is_19_34_or_greater) {
            appendAppVersion("19.33.37")
        } else {
            return@execute
        }
    }
}
