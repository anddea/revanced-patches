package app.revanced.patches.reddit.utils.settings

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.reddit.utils.extension.sharedExtensionPatch
import app.revanced.patches.reddit.utils.patch.PatchList
import app.revanced.patches.reddit.utils.patch.PatchList.SETTINGS_FOR_REDDIT
import app.revanced.patches.reddit.utils.resourceid.labelAcknowledgements
import app.revanced.patches.shared.sharedSettingFingerprint
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import kotlin.io.path.exists

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$EXTENSION_PATH/settings/ActivityHook;->initialize(Landroid/app/Activity;)V"

private lateinit var acknowledgementsLabelBuilderMethod: MutableMethod
private lateinit var settingsStatusLoadMethod: MutableMethod

private val settingsBytecodePatch = bytecodePatch(
    description = "settingsBytecodePatch"
) {

    execute {
        /**
         * Set SharedPrefCategory
         */
        sharedSettingFingerprint.methodOrThrow().apply {
            val stringIndex = indexOfFirstInstructionOrThrow(Opcode.CONST_STRING)
            val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

            replaceInstruction(
                stringIndex,
                "const-string v$stringRegister, \"reddit_revanced\""
            )
        }

        /**
         * Replace settings label
         */
        acknowledgementsLabelBuilderMethod = acknowledgementsLabelBuilderFingerprint
            .methodOrThrow()

        /**
         * Initialize settings activity
         */
        ossLicensesMenuActivityOnCreateFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.startIndex + 1

                addInstructions(
                    insertIndex, """
                        invoke-static {p0}, $EXTENSION_METHOD_DESCRIPTOR
                        return-void
                        """
                )
            }
        }

        settingsStatusLoadMethod = settingsStatusLoadFingerprint.methodOrThrow()
    }
}

internal fun updateSettingsLabel(label: String) =
    acknowledgementsLabelBuilderMethod.apply {
        val insertIndex =
            indexOfFirstLiteralInstructionOrThrow(labelAcknowledgements) + 3
        val insertRegister =
            getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

        addInstruction(
            insertIndex,
            "const-string v$insertRegister, \"$label\""
        )
    }

internal fun updatePatchStatus(description: String) =
    settingsStatusLoadMethod.addInstruction(
        0,
        "invoke-static {}, $EXTENSION_PATH/settings/SettingsStatus;->$description()V"
    )

internal fun updatePatchStatus(patch: PatchList) {
    patch.included = true
}

internal fun updatePatchStatus(
    description: String,
    patch: PatchList
) {
    updatePatchStatus(description)
    updatePatchStatus(patch)
}

private const val DEFAULT_LABEL = "ReVanced Extended"

val settingsPatch = resourcePatch(
    SETTINGS_FOR_REDDIT.title,
    SETTINGS_FOR_REDDIT.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedExtensionPatch,
        settingsBytecodePatch
    )

    val settingsLabelOption = stringOption(
        key = "settingsLabel",
        default = DEFAULT_LABEL,
        title = "RVX settings menu name",
        description = "The name of the RVX settings menu.",
        required = true
    )

    execute {
        /**
         * Replace settings icon and label
         */
        val settingsLabel = settingsLabelOption
            .valueOrThrow()

        arrayOf("preferences.xml", "preferences_logged_in.xml").forEach { targetXML ->
            val resDirectory = get("res")
            val targetXml = resDirectory.resolve("xml").resolve(targetXML).toPath()

            if (!targetXml.exists())
                throw PatchException("The preferences can not be found.")

            val preference = get("res/xml/$targetXML")

            preference.writeText(
                preference.readText()
                    .replace(
                        "\"@drawable/icon_text_post\" android:title=\"@string/label_acknowledgements\"",
                        "\"@drawable/icon_beta_planet\" android:title=\"$settingsLabel\""
                    )
            )
        }

        updateSettingsLabel(settingsLabel)
        updatePatchStatus(SETTINGS_FOR_REDDIT)
    }
}
