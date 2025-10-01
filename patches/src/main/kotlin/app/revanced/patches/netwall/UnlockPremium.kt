package app.revanced.patches.netwall

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import org.w3c.dom.Element
import org.w3c.dom.Node
import premiumCheckFingerprint

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
    }
}

@Suppress("unused")
val unlockPremiumResourcePatch = resourcePatch(
    name = "Unlock Premium",
    description = "Unlocks NetWall Premium features once 'Unlock Premium Now' button is clicked.",
) {
    compatibleWith("com.ysy.app.firewall")
    dependsOn(unlockPremiumPatch)

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
    }
}
