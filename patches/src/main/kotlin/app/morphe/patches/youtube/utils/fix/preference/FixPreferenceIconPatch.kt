/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.utils.fix.preference

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.settingmenu.findPreferenceFingerprint
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import app.morphe.util.fingerprint.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS =
    "$GENERAL_PATH/FixPreferenceIconPatch;"

private const val RVX_SETTINGS_KEY = "revanced_settings_key"

/**
 * Fixes broken preference icons in the old AndroidX settings screen.
 *
 * Based on Morphe's preference icon fix, with RVX's own settings icon preserved.
 */
internal val fixPreferenceIconPatch = bytecodePatch {
    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    execute {
        val setPreferenceIconMethod = SetPreferenceIconFingerprint.method
        val setPreferenceIconSpaceReservedMethodCall = SetPreferenceIconSpaceReservedFingerprint.method
        val findPreferenceMethodCall = findPreferenceFingerprint.methodCall()

        val setPreferenceIconResourceMethod: MutableMethod
        val getPreferenceKeyMethod: MutableMethod
        val helperMethod: MutableMethod

        SetPreferenceIconFingerprint.let {
            val iconDrawableField = setPreferenceIconMethod.implementation!!.instructions
                .first { instruction ->
                    instruction.opcode == Opcode.IPUT_OBJECT &&
                            instruction.getReference<FieldReference>()?.type == "Landroid/graphics/drawable/Drawable;"
                }.getReference<FieldReference>()!!

            val iconResourceField = setPreferenceIconMethod.implementation!!.instructions
                .first { instruction ->
                    instruction.opcode == Opcode.IPUT &&
                            instruction.getReference<FieldReference>()?.type == "I"
                }.getReference<FieldReference>()!!

            val notifyPreferenceChangedMethod = setPreferenceIconMethod.implementation!!.instructions
                .first { instruction ->
                    val reference = instruction.getReference<MethodReference>()
                    instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                            reference != null &&
                            reference.definingClass == setPreferenceIconMethod.definingClass &&
                            reference.parameterTypes.isEmpty() &&
                            reference.returnType == "V"
                }.getReference<MethodReference>()!!

            it.classDef.apply {
                setPreferenceIconResourceMethod = ImmutableMethod(
                    type,
                    "patch_setPreferenceIconResource",
                    listOf(ImmutableMethodParameter("I", annotations, null)),
                    "V",
                    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                    annotations,
                    null,
                    MutableMethodImplementation(3),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                            iput p1, p0, $iconResourceField
                            const/4 v0, 0x0
                            iput-object v0, p0, $iconDrawableField
                            invoke-virtual {p0}, $notifyPreferenceChangedMethod
                            return-void
                        """
                    )
                }

                methods.add(setPreferenceIconResourceMethod)
            }
        }

        PreferenceKeyFingerprint.let {
            val preferenceKeyField = it.instructionMatches.first()
                .instruction.getReference<FieldReference>()!!

            it.classDef.apply {
                getPreferenceKeyMethod = ImmutableMethod(
                    type,
                    "patch_getPreferenceKey",
                    listOf(),
                    "Ljava/lang/String;",
                    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                    annotations,
                    null,
                    MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                            iget-object v0, p0, $preferenceKeyField
                            return-object v0
                        """
                    )
                }

                methods.add(getPreferenceKeyMethod)
            }
        }

        FindPreferenceByIndexFingerprint.let {
            val getAllPreferenceField = it.instructionMatches.last()
                .instruction.getReference<FieldReference>()!!

            it.classDef.apply {
                helperMethod = ImmutableMethod(
                    type,
                    "patch_removePreferenceIcon",
                    listOf(),
                    "V",
                    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                    annotations,
                    null,
                    MutableMethodImplementation(7),
                ).toMutable().apply {
                    addInstructionsWithLabels(
                        0,
                        """
                            invoke-static { }, $EXTENSION_CLASS->removePreferenceIcon()Z
                            move-result v0

                            if-eqz v0, :exit
                            const-string v4, "$RVX_SETTINGS_KEY"
                            invoke-virtual { p0, v4 }, $findPreferenceMethodCall
                            move-result-object v4

                            iget-object v0, p0, $getAllPreferenceField
                            invoke-interface { v0 }, Ljava/util/List;->iterator()Ljava/util/Iterator;
                            move-result-object v1

                            :loop
                            invoke-interface { v1 }, Ljava/util/Iterator;->hasNext()Z
                            move-result v2

                            if-eqz v2, :exit
                            invoke-interface { v1 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
                            move-result-object v2
                            instance-of v3, v2, Landroidx/preference/Preference;

                            if-eqz v3, :loop
                            check-cast v2, Landroidx/preference/Preference;

                            if-eq v2, v4, :loop

                            invoke-virtual { v2 }, $getPreferenceKeyMethod
                            move-result-object v5

                            invoke-static { v5 }, $EXTENSION_CLASS->getPreferenceIconResourceIdentifier(Ljava/lang/String;)I
                            move-result v5

                            if-eqz v5, :remove_icon

                            # setIconSpaceReserved(true).
                            const/4 v3, 0x1
                            invoke-virtual { v2, v3 }, $setPreferenceIconSpaceReservedMethodCall

                            # setIcon(generatedIcon).
                            invoke-virtual { v2, v5 }, $setPreferenceIconResourceMethod

                            goto :loop

                            :remove_icon
                            # setIconSpaceReserved(false).
                            const/4 v3, 0x0
                            invoke-virtual { v2, v3 }, $setPreferenceIconSpaceReservedMethodCall

                            # setIcon(null).
                            const/4 v3, 0x0
                            invoke-virtual { v2, v3 }, $setPreferenceIconMethod

                            goto :loop

                            :exit
                            return-void
                        """
                    )
                }

                methods.add(helperMethod)
            }
        }

        PreferenceScreenSyntheticFingerprint.let {
            it.method.apply {
                val getPreferenceScreenIndex = it.instructionMatches[1].index
                val getPreferenceScreenRegister =
                    getInstruction<FiveRegisterInstruction>(getPreferenceScreenIndex).registerC
                val getPreferenceScreenReference =
                    getInstruction<ReferenceInstruction>(getPreferenceScreenIndex).reference

                val insertIndex = it.instructionMatches.last().index
                val preferenceScreenRegister =
                    findFreeRegister(insertIndex, getPreferenceScreenRegister)

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    """
                        invoke-virtual { v$getPreferenceScreenRegister }, $getPreferenceScreenReference
                        move-result-object v$preferenceScreenRegister

                        if-eqz v$preferenceScreenRegister, :ignore

                        invoke-virtual { v$preferenceScreenRegister }, $helperMethod

                        :ignore
                        nop
                    """
                )
            }
        }
    }
}
