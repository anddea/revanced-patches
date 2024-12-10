package app.revanced.patches.reddit.misc.openlink

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.OPEN_LINKS_DIRECTLY
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.fingerprint.methodOrThrow

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/OpenLinksDirectlyPatch;" +
            "->" +
            "parseRedirectUri(Landroid/net/Uri;)Landroid/net/Uri;"

@Suppress("unused")
val openLinksDirectlyPatch = bytecodePatch(
    OPEN_LINKS_DIRECTLY.title,
    OPEN_LINKS_DIRECTLY.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        screenNavigatorFingerprint.methodOrThrow().addInstructions(
            0, """
                invoke-static {p2}, $EXTENSION_METHOD_DESCRIPTOR
                move-result-object p2
                """
        )

        updatePatchStatus(
            "enableOpenLinksDirectly",
            OPEN_LINKS_DIRECTLY
        )
    }
}
