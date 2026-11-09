/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.utils.fix.preference

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.cloneParameters
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.p0Register
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.settingmenu.PreferenceGroupFindPreferenceFingerprint
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import java.lang.ref.WeakReference

private const val EXTENSION_CLASS =
    "$GENERAL_PATH/FixPreferenceIconPatch;"

private lateinit var preferenceScreenMethodRef: WeakReference<MutableMethod>
private lateinit var fragmentField: FieldReference
private lateinit var getPreferenceScreenMethod: MethodReference

/**
 * Hook of the method that runs after the YouTube settings screen is built.
 */
internal val preferenceScreenHookPatch = bytecodePatch {
    execute {
        PreferenceScreenSyntheticFingerprint.let {
            getPreferenceScreenMethod = it.instructionMatches[1].methodReference
            fragmentField = it.classDef.fields.single { field ->
                field.type == getPreferenceScreenMethod.definingClass
            }

            // 21.45+ reuses the fragment register and p0 before returning,
            // so p0 is kept unchanged and the fragment is read again from it.
            preferenceScreenMethodRef = WeakReference(it.method.cloneParameters())

            // The hooks are added to the cloned method.
            it.clearMatch()
        }
    }
}

/**
 * Adds [instructions] before the settings screen method returns.
 * [instructions] gets the register of the settings screen (not null),
 * and [freeRegisterCount] free registers.
 */
internal fun addPreferenceScreenHook(
    freeRegisterCount: Int,
    instructions: (screenRegister: Int, freeRegisters: List<Int>) -> String
) {
    preferenceScreenMethodRef.get()!!.apply {
        val insertIndex = indexOfFirstInstructionReversedOrThrow { opcode == Opcode.RETURN_VOID }
        val registerProvider = getFreeRegisterProvider(insertIndex, freeRegisterCount + 1, p0Register)
        val screenRegister = registerProvider.getFreeRegister()
        val freeRegisters = List(freeRegisterCount) { registerProvider.getFreeRegister() }

        addInstructionsAtControlFlowLabel(
            insertIndex,
            """
                # p0 can be above v15 after the parameters are cloned.
                move-object/from16 v$screenRegister, p0
                iget-object v$screenRegister, v$screenRegister, $fragmentField
                invoke-virtual { v$screenRegister }, $getPreferenceScreenMethod
                move-result-object v$screenRegister
                if-eqz v$screenRegister, :morphe_preference_screen_hook_exit

                ${instructions(screenRegister, freeRegisters)}

                :morphe_preference_screen_hook_exit
                nop
            """
        )
    }
}

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
        preferenceScreenHookPatch,
    )

    execute {
        val setPreferenceIconMethod = SetPreferenceIconFingerprint.method
        val setPreferenceIconSpaceReservedMethodCall = SetPreferenceIconSpaceReservedFingerprint.method
        val findPreferenceMethodCall = PreferenceGroupFindPreferenceFingerprint.method

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
                .fieldReference

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
                .fieldReference

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
                            const-string v4, "$RVX_SETTINGS_KEY"
                            invoke-virtual { p0, v4 }, $findPreferenceMethodCall
                            move-result-object v4

                            if-eqz v4, :check_legacy_icons
                            invoke-static { }, $EXTENSION_CLASS->getRvxSettingsIconResourceIdentifier()I
                            move-result v5

                            if-eqz v5, :check_legacy_icons
                            # Bind the selected icon in the legacy AndroidX settings screen.
                            const/4 v3, 0x1
                            invoke-virtual { v4, v3 }, $setPreferenceIconSpaceReservedMethodCall
                            invoke-virtual { v4, v5 }, $setPreferenceIconResourceMethod

                            :check_legacy_icons
                            invoke-static { }, $EXTENSION_CLASS->removePreferenceIcon()Z
                            move-result v0

                            if-eqz v0, :exit

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

        addPreferenceScreenHook(0) { screenRegister, _ ->
            "invoke-virtual { v$screenRegister }, $helperMethod"
        }
    }
}
