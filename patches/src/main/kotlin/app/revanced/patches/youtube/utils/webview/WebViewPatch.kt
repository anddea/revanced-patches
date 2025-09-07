package app.revanced.patches.youtube.utils.webview

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.findFreeRegister
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.hookClassHierarchy
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode

private val webViewResourcePatch = resourcePatch(
    description = ""
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
            copyResources("youtube/webview", resourceGroup)
        }
    }
}

val webViewPatch = bytecodePatch(
    description = "webViewPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        webViewResourcePatch,
    )

    execute {
        val hostActivityClass = webViewHostActivityOnCreateFingerprint.mutableClassOrThrow()
        val targetActivityClass = vrWelcomeActivityOnCreateFingerprint.mutableClassOrThrow()

        hookClassHierarchy(
            hostActivityClass,
            targetActivityClass
        )

        vrWelcomeActivityOnCreateFingerprint
            .methodOrThrow()
            .apply {
                val insertIndex =
                    indexOfFirstInstructionOrThrow(Opcode.INVOKE_SUPER) + 1
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
