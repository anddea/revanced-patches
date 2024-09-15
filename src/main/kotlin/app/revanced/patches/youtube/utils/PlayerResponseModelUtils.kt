package app.revanced.patches.youtube.utils

import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object PlayerResponseModelUtils {
    const val PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR =
        "Lcom/google/android/libraries/youtube/innertube/model/player/PlayerResponseModel;"

    fun indexOfPlayerResponseModelInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_INTERFACE &&
                    getReference<MethodReference>()?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
        }
}
