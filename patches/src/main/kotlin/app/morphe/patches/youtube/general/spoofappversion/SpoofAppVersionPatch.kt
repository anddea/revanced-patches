package app.morphe.patches.youtube.general.spoofappversion

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.spoof.appversion.baseSpoofAppVersionPatch
import app.morphe.patches.youtube.utils.CAIRO_FRAGMENT_FEATURE_FLAG
import app.morphe.patches.youtube.utils.cairoFragmentConfigFingerprint
import app.morphe.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.indexOfGetDrawableInstruction
import app.morphe.patches.youtube.utils.patch.PatchList.SPOOF_APP_VERSION
import app.morphe.patches.youtube.utils.playservice.is_19_26_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_34_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.settingsFragment
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.cairoFragmentDisabled
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.settingsFragmentSyntheticFingerprint
import app.morphe.patches.youtube.utils.toolBarButtonFingerprint
import app.morphe.util.Utils.printWarn
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.returnEarly
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
            "20.05.46",
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
    }
}
