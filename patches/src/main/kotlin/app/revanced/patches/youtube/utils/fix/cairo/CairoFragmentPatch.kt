package app.revanced.patches.youtube.utils.fix.cairo

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.playservice.is_19_34_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.util.Utils.printWarn
import app.revanced.util.fingerprint.methodCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.insertNode
import app.revanced.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private var cairoFragmentDisabled = false

private val cairoFragmentBytecodePatch = bytecodePatch(
    description = "cairoFragmentBytecodePatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
        versionCheckPatch
    )

    execute {
        /**
         * Cairo fragment have been widely rolled out in YouTube 19.34+.
         */
        if (!is_19_34_or_greater) {
            return@execute
        }

        // Instead of disabling all Cairo fragment configs,
        // Just disable 'Load Cairo fragment xml' and 'Set style to Cairo preference'.
        fun MutableMethod.disableCairoFragmentConfig() {
            val cairoFragmentConfigMethodCall = cairoFragmentConfigFingerprint
                .methodCall()
            val insertIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.toString() == cairoFragmentConfigMethodCall
            } + 2
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

            addInstruction(insertIndex, "const/4 v$insertRegister, 0x0")
        }

        try {
            arrayOf(
                // Load cairo fragment xml.
                settingsFragmentSyntheticFingerprint
                    .methodOrThrow(),
                // Set style to cairo preference.
                settingsFragmentStylePrimaryFingerprint
                    .methodOrThrow(),
                settingsFragmentStyleSecondaryFingerprint
                    .methodOrThrow(settingsFragmentStylePrimaryFingerprint),
            ).forEach { method ->
                method.disableCairoFragmentConfig()
            }
            cairoFragmentDisabled = true
        } catch (_: Exception) {
            cairoFragmentConfigFingerprint
                .methodOrThrow()
                .returnEarly()

            printWarn("Failed to restore 'Playback' settings. 'Autoplay next video' setting may not appear in the YouTube settings.")
        }
    }
}

/**
 * What [cairoFragmentPatch] does:
 * 1. Disable Cairo fragment settings.
 * 2. Fix - When spoofing the app version to 19.20 or earlier, the app crashes or the Notifications tab is inaccessible.
 * 3. Fix - Preference 'Playback' is hidden.
 * 4. Some settings that were in Preference 'General' are moved to Preference 'Playback'.
 */
val cairoFragmentPatch = resourcePatch(
    description = "cairoFragmentPatch"
) {
    dependsOn(
        cairoFragmentBytecodePatch,
        versionCheckPatch,
    )

    execute {
        /**
         * Cairo fragment have been widely rolled out in YouTube 19.34+.
         */
        if (!is_19_34_or_greater) {
            return@execute
        }

        if (!cairoFragmentDisabled) {
            return@execute
        }

        /**
         * The Preference key for 'Playback' is '@string/playback_key'.
         * Copy the node to add the Preference 'Playback' to the legacy settings fragment.
         */
        document("res/xml/settings_fragment.xml").use { document ->
            val tags = document.getElementsByTagName("Preference")
            List(tags.length) { tags.item(it) as Element }
                .find { it.getAttribute("android:key") == "@string/auto_play_key" }
                ?.let { node ->
                    node.insertNode("Preference", node) {
                        for (index in 0 until node.attributes.length) {
                            with(node.attributes.item(index)) {
                                setAttribute(nodeName, nodeValue)
                            }
                        }
                        setAttribute("android:key", "@string/playback_key")
                    }
                }
        }
    }
}
