package app.revanced.patches.netwall

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.*
import app.revanced.util.*
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element
import org.w3c.dom.Node

private lateinit var context: ResourcePatchContext

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch {
    execute {
        premiumCheckFingerprint.methodOrThrow().apply {
            addInstruction(0, "const/4 p0, 0x7")

            val sputIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.SPUT &&
                        getReference<FieldReference>()?.let {
                            it.name == "g" && it.type == "I" && it.definingClass == definingClass
                        } ?: false
            }

            val constIndex = sputIndex - 1
            val constInstruction = getInstruction(constIndex)

            // Sanity check to ensure we are modifying the correct instruction.
            if (constInstruction.opcode != Opcode.CONST_4 || (constInstruction as NarrowLiteralInstruction).narrowLiteral != 0) {
                throw PatchException("Instruction before 'sput' was not 'const/4 p0, 0x0'. Found: $constInstruction")
            }

            replaceInstruction(constIndex, "const/16 p0, 0x1f4")
        }

        // Remove billing window
        inAppBillingFingerprint.matchOrThrow().let {
            it.method.apply {
                val stringIndex = it.stringMatches!!.first().index
                val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA
                replaceInstruction(stringIndex, """const-string v$stringRegister, "no.billing"""")
            }
        }

        // Remove killProcess to avoid app crash
        integrityCheckFingerprint.methodOrThrow().apply {
            val killProcessIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.toString() == "Landroid/os/Process;->killProcess(I)V"
            }

            val conditionalJumpIndex = indexOfFirstInstructionReversedOrThrow(killProcessIndex) {
                opcode == Opcode.IF_EQZ
            }

            replaceInstruction(conditionalJumpIndex, "nop")
        }
    }
}

@Suppress("unused")
private val unlockPremiumRawResourcePatch = rawResourcePatch(
    description = "unlockPremiumRawResourcePatch"
) {
    execute {
        context = this
    }
}

@Suppress("unused")
val unlockPremiumResourcePatch = resourcePatch(
    name = "Unlock Premium",
    description = "Unlocks NetWall Premium features once 'Unlock Premium Now' button is clicked.",
) {
    compatibleWith("com.ysy.app.firewall")
    dependsOn(unlockPremiumPatch, unlockPremiumRawResourcePatch)

    execute {
        document("AndroidManifest.xml").use { document ->
            val nodesToRemove = mutableListOf<Node>()
            val tagNames = listOf("activity", "provider")

            tagNames.forEach { tagName ->
                val elements = document.getElementsByTagName(tagName)
                for (i in 0 until elements.length) {
                    val node = elements.item(i)
                    if (node is Element) {
                        val nameAttribute = node.getAttribute("android:name")
                        if (nameAttribute.startsWith("com.pairip")) {
                            nodesToRemove.add(node)
                        }
                    }
                }
            }

            if (nodesToRemove.isNotEmpty()) {
                nodesToRemove.forEach { node ->
                    node.parentNode?.removeChild(node)
                }
            }
        }

        // Decoding resources causes the issue that defaultLocale is not found.
        // Remove it.
        val localeConfigFile = "res/xml/_generated_res_locale_config.xml"
        if (get(localeConfigFile).exists()) {
            document(localeConfigFile).use { document ->
                val localeConfigElement = document.getElementsByTagName("locale-config").item(0) as? Element
                if (localeConfigElement?.hasAttribute("android:defaultLocale") == true) {
                    localeConfigElement.removeAttribute("android:defaultLocale")
                }
            }
        }

        // Replace libnetwall.so with the patched one to avoid app crash.
        with(context) {
            setOf(
                "arm64-v8a",
                // "armeabi-v7a",
                // "x86",
                // "x86_64"
            ).forEach { arch ->
                val architectureDirectory = get("lib/$arch")

                if (architectureDirectory.exists()) {
                    val inputStream = inputStreamFromBundledResourceOrThrow(
                        "shared/netwall/lib",
                        "$arch/libnetwall.so"
                    )
                    FilesCompat.copy(
                        inputStream,
                        architectureDirectory.resolve("libnetwall.so"),
                    )
                }
            }
        }
    }
}
