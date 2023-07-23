package app.revanced.util.bytecode

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

fun Method.isNarrowLiteralExists(value: Int): Boolean {
    return getNarrowLiteralIndex(value) != -1
}

fun Method.isWideLiteralExists(value: Long): Boolean {
    return getWideLiteralIndex(value) != -1
}

fun Method.isWide32LiteralExists(value: Long): Boolean {
    return getWide32LiteralIndex(value) != -1
}

fun Method.getNarrowLiteralIndex(value: Int): Int {
    return implementation?.let {
        it.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.CONST
                    && (instruction as NarrowLiteralInstruction).narrowLiteral == value
        }
    } ?: -1
}

fun Method.getStringIndex(value: String): Int {
    return implementation?.let {
        it.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.CONST_STRING
                    && (instruction as BuilderInstruction21c).reference.toString() == value
        }
    } ?: -1
}

fun Method.getWideLiteralIndex(value: Long): Int {
    return implementation?.let {
        it.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.CONST
                    && (instruction as WideLiteralInstruction).wideLiteral == value
        }
    } ?: -1
}

fun Method.getWide32LiteralIndex(value: Long): Int {
    return implementation?.let {
        it.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.CONST_WIDE_32
                    && (instruction as WideLiteralInstruction).wideLiteral == value
        }
    } ?: -1
}

