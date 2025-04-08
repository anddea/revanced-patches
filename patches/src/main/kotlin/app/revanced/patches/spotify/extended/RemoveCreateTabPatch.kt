package app.revanced.patches.spotify.extended

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.fingerprint
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.getResourceId
import app.revanced.patches.shared.mapping.resourceMappingPatch
import app.revanced.util.containsLiteralInstruction

internal val addCreateTabMethodFingerprint = fingerprint {
    returns("V")
    custom { method, _ ->
        method.containsLiteralInstruction(getResourceId(STRING, "bottom_navigation_bar_create_tab_title"))
    }
}

@Suppress("unused")
val removeCreateTabPatch = bytecodePatch(
    name = "Remove Create tab",
    description = "Removes the 'Create' (Plus) tab from the bottom navigation bar.",
) {
    compatibleWith("com.spotify.music")
    dependsOn(resourceMappingPatch)

    execute {
        addCreateTabMethodFingerprint.method.addInstruction(0, "return-void")
    }
}
