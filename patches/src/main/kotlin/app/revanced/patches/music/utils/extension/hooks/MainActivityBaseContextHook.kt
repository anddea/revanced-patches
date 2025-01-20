package app.revanced.patches.music.utils.extension.hooks

import app.revanced.patches.shared.extension.extensionHook
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private var attachBaseContextIndex = -1

internal val mainActivityBaseContextHook = extensionHook(
    insertIndexResolver = { method ->
        attachBaseContextIndex = method.indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.name == "attachBaseContext"
        }

        attachBaseContextIndex + 1
    },
    contextRegisterResolver = { method ->
        val overrideInstruction =
            method.implementation!!.instructions.elementAt(attachBaseContextIndex)
                    as FiveRegisterInstruction
        "v${overrideInstruction.registerD}"
    },
) {
    returns("V")
    parameters("Landroid/content/Context;")
    custom { method, classDef ->
        classDef.type == "Lcom/google/android/apps/youtube/music/activities/MusicActivity;" &&
                method.name == "attachBaseContext"
    }
}
