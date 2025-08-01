package app.revanced.patches.spotify.misc.privacy

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.spotify.misc.extension.sharedExtensionPatch
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/spotify/misc/privacy/SanitizeSharingLinksPatch;"

private val SHARE_PARAMETERS = setOf("context", "pi", "si", "utm_source")

@Suppress("unused")
val sanitizeSharingLinksPatch = bytecodePatch(
    name = "Sanitize sharing links",
    description = "Removes the tracking query parameters from links before they are shared.",
) {
    compatibleWith("com.spotify.music")

    dependsOn(sharedExtensionPatch)

    val shareParameters by stringOption(
        key = "shareParameters",
        default = SHARE_PARAMETERS.joinToString(", "),
        title = "Parameters to remove",
        description = "A list of parameters to be removed from sharing links, separated by commas.",
        required = true,
    )

    execute {
        val extensionMethodDescriptor = "$EXTENSION_CLASS_DESCRIPTOR->" +
                "sanitizeUrl(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"

        val copyFingerprint = if (shareCopyUrlFingerprint.originalMethodOrNull != null) {
            shareCopyUrlFingerprint
        } else {
            oldShareCopyUrlFingerprint
        }

        copyFingerprint.method.apply {
            val newPlainTextInvokeIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "newPlainText"
            }
            val register = getInstruction<FiveRegisterInstruction>(newPlainTextInvokeIndex).registerD
            val freeRegister = getInstruction<OneRegisterInstruction>(newPlainTextInvokeIndex - 1).registerA

            addInstructions(
                newPlainTextInvokeIndex,
                """
                    const-string v$freeRegister, "$shareParameters"
                    invoke-static { v$register, v$freeRegister }, $extensionMethodDescriptor
                    move-result-object v$register
                """
            )
        }

        // Android native share sheet is used for all other quick share types (X, WhatsApp, etc).
        val shareUrlParameter: String
        val shareSheetFingerprint = if (formatAndroidShareSheetUrlFingerprint.originalMethodOrNull != null) {
            val methodAccessFlags = formatAndroidShareSheetUrlFingerprint.originalMethod
            shareUrlParameter = if (AccessFlags.STATIC.isSet(methodAccessFlags.accessFlags)) {
                // In newer implementations the method is static, so p0 is not `this`.
                "p1"
            } else {
                // In older implementations the method is not static, making it so p0 is `this`.
                // For that reason, add one to the parameter register.
                "p2"
            }

            formatAndroidShareSheetUrlFingerprint
        } else {
            shareUrlParameter = "p2"
            oldFormatAndroidShareSheetUrlFingerprint
        }

        shareSheetFingerprint.method.apply {
            val freeRegister = getInstruction<OneRegisterInstruction>(0).registerA

            addInstructions(
                0,
                """
                    const-string v$freeRegister, "$shareParameters"
                    invoke-static { $shareUrlParameter, v$freeRegister }, $extensionMethodDescriptor
                    move-result-object $shareUrlParameter
                """
            )
        }
    }
}
