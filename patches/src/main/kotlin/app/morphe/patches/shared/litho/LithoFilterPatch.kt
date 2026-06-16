/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.shared.litho

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.conversionContextFingerprintToString2
import app.morphe.patches.shared.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playservice.*
import app.morphe.util.*
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.util.MethodUtil

lateinit var addLithoFilter: (String) -> Unit
    private set

private const val EXTENSION_LITHO_FILTER_CLASS_DESCRIPTOR = "$COMPONENTS_PATH/LithoFilterPatch;"
private const val EXTENSION_LEGACY_LITHO_FILTER_CLASS_DESCRIPTOR = "$COMPONENTS_PATH/LegacyLithoFilterPatch;"
internal const val EXTENSION_FILTER_ARRAY_DESCRIPTOR = "[$COMPONENTS_PATH/Filter;"
internal var emptyComponentLabel = ""

// Registers used in extension helperMethod.
private const val REGISTER_FILTER_CLASS = 0
private const val REGISTER_FILTER_COUNT = 1
private const val REGISTER_FILTER_ARRAY = 2

private lateinit var helperMethod: MutableMethod

// For now, we'll use hybrid implementation, LithoFilter for YouTube based on ReVanced, for YTM based on RVX
val lithoFilterPatch = bytecodePatch(
    description = "Hooks the method which parses the bytes into a ComponentContext to filter components.",
) {
    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    var filterCount = 0
    var isYouTube = false
    var filterArrayMethod: MutableMethod? = null
    val filterClassDescriptors = mutableSetOf<String>()

    execute {
        // `componentContextSubParserFingerprint` is specific to the YouTube app.
        isYouTube = ConversionContextFingerprintToString.originalClassDefOrNull != null && is_20_22_or_greater
        // print("isYouTube: $isYouTube\n")

        if (isYouTube) {
            // Remove dummy filter from extension static field
            // and add the filters included during patching.
            LithoFilterFingerprint.match(classDefBy(EXTENSION_LITHO_FILTER_CLASS_DESCRIPTOR)).let {
                it.method.apply {
                    // Add a helper method to avoid finding multiple free registers.
                    // This fixes an issue with extension compiled with Android Gradle Plugin 8.3.0+.
                    val helperClass = definingClass
                    val helperName = "patch_getFilterArray"
                    val helperReturnType = EXTENSION_FILTER_ARRAY_DESCRIPTOR
                    helperMethod = ImmutableMethod(
                        helperClass,
                        helperName,
                        listOf(),
                        helperReturnType,
                        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addLithoFilter = { classDescriptor ->
                            if (filterClassDescriptors.add(classDescriptor)) {
                                addInstructions(
                                    0,
                                    """
                                    new-instance v$REGISTER_FILTER_CLASS, $classDescriptor
                                    invoke-direct { v$REGISTER_FILTER_CLASS }, $classDescriptor-><init>()V
                                    const/16 v$REGISTER_FILTER_COUNT, ${filterCount++}
                                    aput-object v$REGISTER_FILTER_CLASS, v$REGISTER_FILTER_ARRAY, v$REGISTER_FILTER_COUNT
                                """
                                )
                            }
                        }
                    }
                    it.classDef.methods.add(helperMethod)

                    val insertIndex = it.instructionMatches.first().index
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                        invoke-static {}, $EXTENSION_LITHO_FILTER_CLASS_DESCRIPTOR->$helperName()$EXTENSION_FILTER_ARRAY_DESCRIPTOR
                        move-result-object v$insertRegister
                        """
                    )
                }
            }

            // region Pass the buffer into extension

            if (is_20_22_or_greater) {
                // Hook method that bridges between UPB buffer native code and FB Litho.
                // Method is found in 19.25+, but is forcefully turned off for 20.21 and lower.
                ProtobufBufferReferenceFingerprint.let {
                    // Hook the buffer after the call to jniDecode().
                    it.method.addInstruction(
                        it.instructionMatches.last().index + 1,
                        "invoke-static { p1 }, $EXTENSION_LITHO_FILTER_CLASS_DESCRIPTOR->setProtoBuffer([B)V",
                    )
                }
            }

            // Legacy Non native buffer.
            ProtobufBufferReferenceLegacyFingerprint.method.addInstruction(
                0,
                "invoke-static { p2 }, $EXTENSION_LITHO_FILTER_CLASS_DESCRIPTOR->setProtoBuffer(Ljava/nio/ByteBuffer;)V",
            )

            val protoBufferEncodeMethod = ProtobufBufferEncodeFingerprint.method

            // endregion


            // region Modify the create component method and
            // if the component is filtered then return an empty component.

            // Find the identifier/path fields of the conversion context.

            val conversionContextIdentifierField = ConversionContextFingerprintToString.method
                .findFieldFromToString("identifierProperty=")

            val conversionContextPathBuilderField = ConversionContextFingerprintToString.originalClassDef
                .fields.single { field -> field.type == "Ljava/lang/StringBuilder;" }

            // Find class and methods to create an empty component.
            val builderMethodDescriptor = EmptyComponentFingerprint.classDef.methods.single { method ->
                // The only static method in the class.
                AccessFlags.STATIC.isSet(method.accessFlags)
            }

            val emptyComponentField = classDefBy(builderMethodDescriptor.returnType).fields.single()

            // Find the method call that gets the value of 'buttonViewModel.accessibilityId'.
            val accessibilityIdMethod = with(AccessibilityIdFingerprint) {
                val index = instructionMatches.first().index
                method.getInstruction<ReferenceInstruction>(index).reference as MethodReference
            }

            // There's a method in the same class that gets the value of 'buttonViewModel.accessibilityText'.
            // As this class is abstract, we need to find another method that uses a method call.
            val accessibilityTextFingerprint = Fingerprint(
                returnType = "V",
                filters = listOf(
                    methodCall(
                        opcode = Opcode.INVOKE_INTERFACE,
                        parameters = listOf(),
                        returnType = "Ljava/lang/String;"
                    ),
                    methodCall(
                        reference = accessibilityIdMethod,
                        location = MatchAfterWithin(5)
                    )
                ),
                custom = { method, _ ->
                    // 'public final synthetic' or 'public final bridge synthetic'.
                    AccessFlags.SYNTHETIC.isSet(method.accessFlags)
                }
            )

            // Find the method call that gets the value of 'buttonViewModel.accessibilityText'.
            val accessibilityTextMethod = with (accessibilityTextFingerprint) {
                val index = instructionMatches.first().index
                method.getInstruction<ReferenceInstruction>(index).reference as MethodReference
            }

            ComponentCreateFingerprint.method.apply {
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.RETURN_OBJECT)


                // We can directly access the class related with the buttonViewModel from this method.
                // This is within 10 lines of insertIndex.
                val buttonViewModelIndex = indexOfFirstInstructionReversedOrThrow(insertIndex) {
                    opcode == Opcode.CHECK_CAST &&
                            getReference<TypeReference>()?.type == accessibilityIdMethod.definingClass
                }
                val buttonViewModelRegister =
                    getInstruction<OneRegisterInstruction>(buttonViewModelIndex).registerA
                val accessibilityIdIndex = buttonViewModelIndex + 2

                // This is an index that checks if there is accessibility-related text.
                // This is within 10 lines of buttonViewModelIndex.
                val nullCheckIndex = indexOfFirstInstructionReversedOrThrow(
                    buttonViewModelIndex, Opcode.IF_EQZ
                )

                val registerProvider = getFreeRegisterProvider(
                    insertIndex, 4, buttonViewModelRegister
                )
                val freeRegister = registerProvider.getFreeRegister()
                val identifierRegister = registerProvider.getFreeRegister()
                val pathRegister = registerProvider.getFreeRegister()
                val bufferRegister = registerProvider.getFreeRegister()

                // We need to find a free register to store the accessibilityId and accessibilityText.
                // This is before the insertion index.
                val accessibilityRegisterProvider = getFreeRegisterProvider(
                    nullCheckIndex,
                    2,
                    registerProvider.getUsedAndUnAvailableRegisters()
                )
                val accessibilityIdRegister = accessibilityRegisterProvider.getFreeRegister()
                val accessibilityTextRegister = accessibilityRegisterProvider.getFreeRegister()

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    """
                    move-object/from16 v$bufferRegister, p3

                    # Use the current component buffer directly. Reusing buffers by identifier can
                    # collide for repeated components such as fullscreen quick action buttons.
                    instance-of v$freeRegister, v$bufferRegister, ${protoBufferEncodeMethod.definingClass}
                    if-eqz v$freeRegister, :empty_buffer

                    check-cast v$bufferRegister, ${protoBufferEncodeMethod.definingClass}
                    invoke-virtual { v$bufferRegister }, $protoBufferEncodeMethod
                    move-result-object v$bufferRegister
                    goto :hook

                    :empty_buffer
                    const/4 v$freeRegister, 0x0
                    new-array v$bufferRegister, v$freeRegister, [B

                    :hook
                    invoke-static { v$bufferRegister }, $EXTENSION_LITHO_FILTER_CLASS_DESCRIPTOR->setDirectProtoBuffer([B)V

                    move-object/from16 v$freeRegister, p2
                    
                    # 20.41 field is the abstract superclass.
                    # Verify it's the expected subclass just in case. 
                    instance-of v$identifierRegister, v$freeRegister, ${ConversionContextFingerprintToString.classDef.type}
                    if-eqz v$identifierRegister, :unfiltered
                    
                    iget-object v$identifierRegister, v$freeRegister, $conversionContextIdentifierField
                    iget-object v$pathRegister, v$freeRegister, $conversionContextPathBuilderField
                    invoke-static { v$identifierRegister, v$accessibilityIdRegister, v$accessibilityTextRegister, v$pathRegister, v$freeRegister }, $EXTENSION_LITHO_FILTER_CLASS_DESCRIPTOR->isFiltered(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/StringBuilder;Ljava/lang/Object;)Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :unfiltered
                    
                    # Return an empty component
                    move-object/from16 v$freeRegister, p1
                    invoke-static { v$freeRegister }, $builderMethodDescriptor
                    move-result-object v$freeRegister
                    iget-object v$freeRegister, v$freeRegister, $emptyComponentField
                    return-object v$freeRegister
        
                    :unfiltered
                    nop
                """
                )

                // If there is text related to accessibility, get the accessibilityId and accessibilityText.
                addInstructions(
                    accessibilityIdIndex,
                    """
                    # Get accessibilityId
                    invoke-interface { v$buttonViewModelRegister }, $accessibilityIdMethod
                    move-result-object v$accessibilityIdRegister
                    
                    # Get accessibilityText
                    invoke-interface { v$buttonViewModelRegister }, $accessibilityTextMethod
                    move-result-object v$accessibilityTextRegister
                """
                )

                // If there is no accessibility-related text,
                // both accessibilityId and accessibilityText use empty values.
                addInstructions(
                    nullCheckIndex,
                    """
                    const-string v$accessibilityIdRegister, ""
                    const-string v$accessibilityTextRegister, ""
                """
                )
            }

            // endregion


            // region Change Litho thread executor to 1 thread to fix layout issue in unpatched YouTube

            LithoThreadExecutorFingerprint.method.addInstructions(
                0,
                """
                invoke-static { p1 }, $EXTENSION_LITHO_FILTER_CLASS_DESCRIPTOR->getExecutorCorePoolSize(I)I
                move-result p1
                invoke-static { p2 }, $EXTENSION_LITHO_FILTER_CLASS_DESCRIPTOR->getExecutorMaxThreads(I)I
                move-result p2
            """
            )

            // endregion


            // region A/B test of new Litho native code

            // Turn off native code that handles litho component names.  If this feature is on then nearly
            // all litho components have a null name and identifier/path filtering is completely broken.
            //
            // Flag was removed in 20.05. It appears a new flag might be used instead (45660109L),
            // but if the flag is forced on then litho filtering still works correctly.
            if (is_19_25_or_greater && !is_20_05_or_greater) {
                LithoComponentNameUpbFeatureFlagFingerprint.method.returnLate(false)
            }

            // Turn off a feature flag that enables native code of protobuf parsing (Upb protobuf).
            LithoConverterBufferUpbFeatureFlagFingerprint.let {
                // 20.22 the flag is still enabled in one location, but what it does is not known.
                // Disable it anyway.
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    false
                )
            }

            // endregion
        } else {
            // region Pass the buffer into extension

            byteBufferFingerprint.methodOrThrow().addInstruction(
                0,
                "invoke-static { p2 }, $EXTENSION_LEGACY_LITHO_FILTER_CLASS_DESCRIPTOR->setProtoBuffer(Ljava/nio/ByteBuffer;)V"
            )

            // endregion

            // region Hook the method that parses bytes into a ComponentContext

            // Allow the method to run to completion, and override the
            // return value with an empty component if it should be filtered.
            // It is important to allow the original code to always run to completion,
            // otherwise high memory usage and poor app performance can occur.

            // Find the identifier/path fields of the conversion context.
            val conversionContextClass = conversionContextFingerprintToString2
                .matchOrThrow()
                .originalClassDef

            val conversionContextIdentifierField = componentContextParserFingerprint2.matchOrThrow().let {
                // Identifier field is loaded just before the string declaration.
                val index = it.method.indexOfFirstInstructionReversedOrThrow(
                    it.stringMatches.first().index
                ) {
                    val reference = getReference<FieldReference>()
                    reference?.definingClass == conversionContextClass.type
                            && reference.type == "Ljava/lang/String;"
                }

                it.method.getInstruction<ReferenceInstruction>(index).getReference<FieldReference>()!!
            }

            val conversionContextPathBuilderField = conversionContextClass
                .fields.single { field -> field.type == "Ljava/lang/StringBuilder;" }

            // Find class and methods to create an empty component.
            val builderMethodDescriptor = emptyComponentFingerprint2
                .mutableClassOrThrow()
                .methods
                .single {
                    // The only static method in the class.
                        method ->
                    AccessFlags.STATIC.isSet(method.accessFlags)
                }
            val emptyComponentField = classDefBy {
                // Only one field that matches.
                it.type == builderMethodDescriptor.returnType
            }.fields.single()

            emptyComponentLabel = """
            move-object/from16 v0, p1
            invoke-static {v0}, $builderMethodDescriptor
            move-result-object v0
            iget-object v0, v0, $emptyComponentField
            return-object v0
            """

            var isLegacyMethod = false

            try {
                isLegacyMethod = MethodUtil.methodSignaturesMatch(
                    componentContextParserLegacyFingerprint.methodOrThrow(),
                    componentContextSubParserFingerprint2.methodOrThrow()
                )
            } catch (_: Exception) {
            }

            componentCreateFingerprint.methodOrThrow().apply {
                val insertIndex = if (isLegacyMethod) {
                    // YT 19.16 and YTM 6.51 clobbers p2 so must check at start of the method and not at the return index.
                    0
                } else {
                    indexOfFirstInstructionOrThrow(Opcode.RETURN_OBJECT)
                }

                val freeRegister = findFreeRegister(insertIndex)
                val identifierRegister = findFreeRegister(insertIndex, freeRegister)
                val pathRegister = findFreeRegister(insertIndex, freeRegister, identifierRegister)

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    """
                    move-object/from16 v$freeRegister, p2

                    # Required for YouTube Music.
                    check-cast v$freeRegister, ${conversionContextIdentifierField.definingClass}

                    iget-object v$identifierRegister, v$freeRegister, $conversionContextIdentifierField
                    iget-object v$pathRegister, v$freeRegister, $conversionContextPathBuilderField
                    invoke-static {v$pathRegister, v$identifierRegister, v$freeRegister}, $EXTENSION_LEGACY_LITHO_FILTER_CLASS_DESCRIPTOR->isFiltered(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;)Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :unfiltered
                    """ + emptyComponentLabel + """
                    :unfiltered
                    nop
                    """
                )
            }

            // endregion

            // region Change Litho thread executor to 1 thread to fix layout issue in unpatched YouTube

            lithoThreadExecutorFingerprint.methodOrThrow().addInstructions(
                0, """
                invoke-static { p1 }, $EXTENSION_LEGACY_LITHO_FILTER_CLASS_DESCRIPTOR->getExecutorCorePoolSize(I)I
                move-result p1
                invoke-static { p2 }, $EXTENSION_LEGACY_LITHO_FILTER_CLASS_DESCRIPTOR->getExecutorMaxThreads(I)I
                move-result p2
                """
            )

            // endregion

            // region A/B test of new Litho native code

            // Turn off native code that handles litho component names.  If this feature is on then nearly
            // all litho components have a null name and identifier/path filtering is completely broken.

            mapOf(
                bufferUpbFeatureFlagFingerprint to BUFFER_UPD_FEATURE_FLAG,
                pathUpbFeatureFlagFingerprint to PATH_UPD_FEATURE_FLAG,
            ).forEach { (fingerprint, literalValue) ->
                if (fingerprint.second.methodOrNull != null) {
                    fingerprint.injectLiteralInstructionBooleanCall(
                        literalValue,
                        "0x0"
                    )
                }
            }

            // endregion

            // Create a new method to get the filter array to avoid register conflicts.
            // This fixes an issue with extension compiled with Android Gradle Plugin 8.3.0+.
            // https://github.com/ReVanced/revanced-patches/issues/2818
            val lithoFilterMethods = findMethodsOrThrow(EXTENSION_LEGACY_LITHO_FILTER_CLASS_DESCRIPTOR)

            lithoFilterMethods
                .first { it.name == "<clinit>" }
                .apply {
                    val setArrayIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.SPUT_OBJECT &&
                                getReference<FieldReference>()?.type == EXTENSION_FILTER_ARRAY_DESCRIPTOR
                    }
                    val setArrayRegister =
                        getInstruction<OneRegisterInstruction>(setArrayIndex).registerA
                    val addedMethodName = "getFilterArray"

                    addInstructions(
                        setArrayIndex, """
                        invoke-static {}, $EXTENSION_LEGACY_LITHO_FILTER_CLASS_DESCRIPTOR->$addedMethodName()$EXTENSION_FILTER_ARRAY_DESCRIPTOR
                        move-result-object v$setArrayRegister
                        """
                    )

                    filterArrayMethod = ImmutableMethod(
                        definingClass,
                        addedMethodName,
                        emptyList(),
                        EXTENSION_FILTER_ARRAY_DESCRIPTOR,
                        AccessFlags.PRIVATE or AccessFlags.STATIC,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstruction(
                            0,
                            "return-object v2"
                        )
                    }

                    lithoFilterMethods.add(filterArrayMethod)
                }

            addLithoFilter = { classDescriptor ->
                if (filterClassDescriptors.add(classDescriptor)) {
                    filterArrayMethod!!.addInstructions(
                        0,
                        """
                        new-instance v0, $classDescriptor
                        invoke-direct {v0}, $classDescriptor-><init>()V
                        const/16 v1, ${filterCount++}
                        aput-object v0, v2, v1
                        """
                    )
                }
            }
        }
    }

    finalize {
        if (isYouTube) {
            helperMethod.apply {
                addInstruction(
                    implementation!!.instructions.size,
                    "return-object v$REGISTER_FILTER_ARRAY"
                )

                addInstructions(
                    0,
                    """
                    const/16 v$REGISTER_FILTER_COUNT, $filterCount
                    new-array v$REGISTER_FILTER_ARRAY, v$REGISTER_FILTER_COUNT, $EXTENSION_FILTER_ARRAY_DESCRIPTOR
                """
                )
            }
        } else {
            filterArrayMethod?.addInstructions(
                0, """
                    const/16 v0, $filterCount
                    new-array v2, v0, $EXTENSION_FILTER_ARRAY_DESCRIPTOR
                """
            )
        }
    }
}
