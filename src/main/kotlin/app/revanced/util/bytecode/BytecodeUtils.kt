package app.revanced.util.bytecode

import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction

fun Method.getNarrowLiteralIndex(value: Int): Int {
    return implementation?.let {
        it.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.CONST && (instruction as NarrowLiteralInstruction).narrowLiteral == value
        }
    } ?: -1
}

fun Method.isNarrowLiteralExists(value: Int): Boolean {
    return getNarrowLiteralIndex(value) != -1
}

fun Method.getWideLiteralIndex(value: Long): Int {
    return implementation?.let {
        it.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.CONST && (instruction as WideLiteralInstruction).wideLiteral == value
        }
    } ?: -1
}

fun Method.isWideLiteralExists(value: Long): Boolean {
    return getWideLiteralIndex(value) != -1
}
