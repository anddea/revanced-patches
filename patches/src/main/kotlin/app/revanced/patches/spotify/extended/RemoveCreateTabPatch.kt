package app.revanced.patches.spotify.extended.branding.name // Or your desired package

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.fingerprint
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.containsLiteralInstruction

internal val addCreateTabMethodFingerprint = fingerprint {
    returns("V")
     custom { method, _ ->
        method.containsLiteralInstruction(0x7f130299L)
     }
}

@Suppress("unused")
val removeCreateTabPatch = bytecodePatch(
    name = "Remove Create tab",
    description = "Removes the 'Create' (Plus) tab from the bottom navigation bar.",
    false,
) {
    compatibleWith("com.spotify.music")

    execute {
        addCreateTabMethodFingerprint.method.addInstruction(0, "return-void")
    }
}
