/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3754
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.general.autocaptions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/**
 * Adds a public final method.
 *
 * @param parameters Object parameter types.
 * @param freeRegisters Number of free registers, starting at v0.
 */
internal fun MutableClass.addHelperMethod(
    name: String,
    returnType: String,
    parameters: List<String> = listOf(),
    freeRegisters: Int = 1,
    smali: String,
) {
    methods.add(
        ImmutableMethod(
            type,
            name,
            parameters.map { ImmutableMethodParameter(it, null, null) },
            returnType,
            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
            null,
            null,
            // Parameter registers are p0 (this) and one for each parameter.
            MutableMethodImplementation(freeRegisters + 1 + parameters.size),
        ).toMutable().apply {
            addInstructions(0, smali)
        }
    )
}

