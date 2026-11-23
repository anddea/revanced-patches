package app.morphe.patches.youtube.utils.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.findFreeRegister
import app.morphe.patches.youtube.utils.resourceid.ytFillBell
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/** 21.39 stores icon maps assembled from pairs instead of adding entries with EnumMap.put(). */
private object NavigationIconMapStoreFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        literal({ ytFillBell }),
        fieldAccess(opcode = Opcode.SPUT_OBJECT, definingClass = "this", type = "Ljava/util/Map;")
    )
)

/** Replace the completed map with a copy, so immutable app maps are supported too. */
context(_: BytecodePatchContext)
internal fun hookNavigationIconMap(enumField: String, callback: String) {
    NavigationIconMapStoreFingerprint.clearMatch()
    NavigationIconMapStoreFingerprint.let {
        val store = it.instructionMatches.last()
        val mapRegister = (store.instruction as OneRegisterInstruction).registerA
        val enumRegister = it.method.findFreeRegister(store.index, mapRegister)
        it.method.addInstructionsAtControlFlowLabel(store.index, """
            sget-object v$enumRegister, $enumField
            invoke-static { v$mapRegister, v$enumRegister }, $callback(Ljava/util/Map;Ljava/lang/Enum;)Ljava/util/Map;
            move-result-object v$mapRegister
        """)
    }
}
