package app.revanced.patches.shared.webview

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.BytecodePatchBuilder
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.extension.Constants.EXTENSION_PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.util.ResourceGroup
import app.revanced.util.className
import app.revanced.util.copyResources
import app.revanced.util.findFreeRegister
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.hookClassHierarchy
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.util.MethodUtil

private val webViewResourcePatch = resourcePatch(
    description = "webViewResourcePatch"
) {
    execute {
        arrayOf(
            ResourceGroup(
                "layout",
                "revanced_webview.xml",
            ),
            ResourceGroup(
                "menu",
                "revanced_webview_menu.xml",
            )
        ).forEach { resourceGroup ->
            copyResources("shared/webview", resourceGroup)
        }
    }
}

fun webViewPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    targetActivityFingerprint: Pair<String, Fingerprint>,
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    description = "webViewPatch",
) {
    block()

    dependsOn(webViewResourcePatch)

    execute {
        val hostActivityClass = webViewHostActivityOnCreateFingerprint.mutableClassOrThrow()
        val targetActivityClass = targetActivityFingerprint.mutableClassOrThrow()

        hookClassHierarchy(
            hostActivityClass,
            targetActivityClass
        )

        targetActivityClass.methods.forEach { method ->
            method.apply {
                if (!MethodUtil.isConstructor(method) && returnType == "V") {
                    val insertIndex =
                        indexOfFirstInstruction(Opcode.INVOKE_SUPER) + 1
                    if (insertIndex > 0) {
                        val freeRegister = findFreeRegister(insertIndex)

                        addInstructionsWithLabels(
                            insertIndex, """
                                invoke-virtual {p0}, ${hostActivityClass.type}->isInitialized()Z
                                move-result v$freeRegister
                                if-eqz v$freeRegister, :ignore
                                return-void
                                :ignore
                                nop
                                """
                        )
                    }
                }
            }
        }

        val targetActivityClassName = targetActivityClass.type.className
        findMethodOrThrow(EXTENSION_PATCH_STATUS_CLASS_DESCRIPTOR) {
            name == "WebViewActivityClass"
        }.returnEarly(targetActivityClassName)

        executeBlock()
    }
}