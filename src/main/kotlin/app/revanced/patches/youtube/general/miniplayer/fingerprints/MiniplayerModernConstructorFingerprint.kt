package app.revanced.patches.youtube.general.miniplayer.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernConstructorFingerprint.constructorMethodCount
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.containsWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("SpellCheckingInspection")
internal object MiniplayerModernConstructorFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("L"),
    customFingerprint = custom@{ methodDef, classDef ->
        if (!methodDef.containsWideLiteralInstructionIndex(45623000)) // Magic number found in the constructor.
            return@custom false

        classDef.methods.forEach {
            if (MethodUtil.isConstructor(it)) constructorMethodCount += 1
        }

        if (!SettingsPatch.upward1925)
            return@custom true

        // Double tap action (Used in YouTube 19.25.39+).
        methodDef.containsWideLiteralInstructionIndex(45628823)
                && methodDef.containsWideLiteralInstructionIndex(45630429)
    }
) {
    private var constructorMethodCount = 0

    internal fun isMultiConstructorMethod() = constructorMethodCount > 1
}