package app.revanced.patches.youtube.general.spoofappversion

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.spoof.appversion.baseSpoofAppVersionPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.indexOfGetDrawableInstruction
import app.revanced.patches.youtube.utils.patch.PatchList.SPOOF_APP_VERSION
import app.revanced.patches.youtube.utils.playservice.is_18_34_or_greater
import app.revanced.patches.youtube.utils.playservice.is_18_39_or_greater
import app.revanced.patches.youtube.utils.playservice.is_18_49_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_23_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.toolBarButtonFingerprint
import app.revanced.util.appendAppVersion
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val spoofAppVersionBytecodePatch = bytecodePatch(
    description = "spoofAppVersionBytecodePatch"
) {

    dependsOn(versionCheckPatch)

    execute {
        if (!is_19_23_or_greater) {
            return@execute
        }

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
    }

}

@Suppress("unused")
val spoofAppVersionPatch = resourcePatch(
    SPOOF_APP_VERSION.title,
    SPOOF_APP_VERSION.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseSpoofAppVersionPatch("$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"),
        spoofAppVersionBytecodePatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        if (is_18_34_or_greater) {
            appendAppVersion("18.33.40")
            if (is_18_39_or_greater) {
                appendAppVersion("18.38.45")
                if (is_18_49_or_greater) {
                    appendAppVersion("18.48.39")
                }
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_APP_VERSION"
            ),
            SPOOF_APP_VERSION
        )

        // endregion

    }
}
