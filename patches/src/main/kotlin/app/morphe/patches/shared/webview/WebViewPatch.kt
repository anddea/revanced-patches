package app.morphe.patches.shared.webview

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.extension.Constants.EXTENSION_PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.util.ResourceGroup
import app.morphe.util.className
import app.morphe.util.copyResources
import app.morphe.util.findFreeRegister
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.hookClassHierarchy
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.returnEarly
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