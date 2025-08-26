package app.revanced.patches.shared.litho

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.conversionContextFingerprintToString2
import app.revanced.patches.shared.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.playservice.*
import app.revanced.util.*
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.util.MethodUtil
import java.util.logging.Logger

lateinit var addLithoFilter: (String) -> Unit
    private set

private const val EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR = "$COMPONENTS_PATH/LithoFilterPatch;"
private const val EXTENSION_FILER_ARRAY_DESCRIPTOR = "[$COMPONENTS_PATH/Filter;"
internal var emptyComponentLabel = ""

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

    execute {
        // `componentContextSubParserFingerprint` is specific to the YouTube app.
        isYouTube = conversionContextFingerprintToString.originalClassDefOrNull != null && is_20_13_or_greater
        // print("isYouTube: $isYouTube\n")

        if (isYouTube) {
            // Remove dummy filter from extension static field
            // and add the filters included during patching.
            lithoFilterFingerprint.method.apply {
                removeInstructions(2, 4) // Remove dummy filter.

                addLithoFilter = { classDescriptor ->
                    addInstructions(
                        2,
                        """
                            new-instance v1, $classDescriptor
                            invoke-direct { v1 }, $classDescriptor-><init>()V
                            const/16 v2, ${filterCount++}
                            aput-object v1, v0, v2
                        """,
                    )
                }
            }

            // region Pass the buffer into extension.

            // if (is_19_25_or_greater) {
            //     // Hook method that bridges between UPB buffer native code and FB Litho.
            //     protobufBufferReferenceFingerprint.let {
            //         // Hook the buffer after the call to jniDecode().
            //         it.method.addInstruction(
            //             it.patternMatch!!.endIndex + 1,
            //             "invoke-static { p1 }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->setProtoBuffer([B)V",
            //         )
            //     }
            // }

            // Legacy non-native buffer.
            protobufBufferReferenceLegacyFingerprint.method.addInstruction(
                0,
                "invoke-static { p2 }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->setProtoBuffer(Ljava/nio/ByteBuffer;)V",
            )

            // endregion

            // region Hook the method that parses bytes into a ComponentContext.

            // Allow the method to run to completion, and override the
            // return value with an empty component if it should be filtered.
            // It is important to allow the original code to always run to completion,
            // otherwise high memory usage and poor app performance can occur.

            // Find the identifier/path fields of the conversion context.
            val conversionContextIdentifierField = componentContextParserFingerprint.let {
                // Identifier field is loaded just before the string declaration.
                val index = it.method.indexOfFirstInstructionReversedOrThrow(
                    it.stringMatches!!.first().index
                ) {
                    val reference = getReference<FieldReference>()
                    reference?.definingClass == conversionContextFingerprintToString.originalClassDef.type
                            && reference.type == "Ljava/lang/String;"
                }

                it.method.getInstruction<ReferenceInstruction>(index).getReference<FieldReference>()!!
            }

            val conversionContextPathBuilderField = conversionContextFingerprintToString.originalClassDef
                .fields.single { field -> field.type == "Ljava/lang/StringBuilder;" }

            // Find class and methods to create an empty component.
            val builderMethodDescriptor = emptyComponentFingerprint.classDef.methods.single {
                // The only static method in the class.
                    method -> AccessFlags.STATIC.isSet(method.accessFlags)
            }
            val emptyComponentField = classBy {
                // Only one field that matches.
                it.type == builderMethodDescriptor.returnType
            }!!.immutableClass.fields.single()

            emptyComponentLabel = """
                move-object/from16 v0, p1
                invoke-static {v0}, $builderMethodDescriptor
                move-result-object v0
                iget-object v0, v0, $emptyComponentField
                return-object v0
            """

            componentCreateFingerprint.methodOrThrow().apply {
                val insertIndex = if (is_19_17_or_greater) {
                    indexOfFirstInstructionOrThrow(Opcode.RETURN_OBJECT)
                } else {
                    // 19.16 clobbers p2 so must check at start of the method and not at the return index.
                    0
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
                        invoke-static {v$pathRegister, v$identifierRegister, v$freeRegister}, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->isFiltered(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;)Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :unfiltered
                        """ + emptyComponentLabel + """
                        :unfiltered
                        nop
                    """
                )
            }

            // endregion


            // region Change Litho thread executor to 1 thread to fix layout issue in unpatched YouTube.

            lithoThreadExecutorFingerprint.methodOrThrow().addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->getExecutorCorePoolSize(I)I
                    move-result p1
                    invoke-static { p2 }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->getExecutorMaxThreads(I)I
                    move-result p2
                """
            )

            // endregion


            // region A/B test of new Litho native code.

            // Turn off native code that handles litho component names.  If this feature is on then nearly
            // all litho components have a null name and identifier/path filtering is completely broken.
            //
            // Flag was removed in 20.05. It appears a new flag might be used instead (45660109L),
            // but if the flag is forced on then litho filtering still works correctly.
            if (is_19_25_or_greater && !is_20_05_or_greater) {
                lithoComponentNameUpbFeatureFlagFingerprint.method.apply {
                    // Don't use return early, so the debug patch logs if this was originally on.
                    val insertIndex = indexOfFirstInstructionOrThrow(Opcode.RETURN)
                    val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstruction(insertIndex, "const/4 v$register, 0x0")
                }
            }

            // Turn off a feature flag that enables native code of protobuf parsing (Upb protobuf).
            // If this is enabled, then the litho protobuffer hook will always show an empty buffer
            // since it's no longer handled by the hooked Java code.
            lithoConverterBufferUpbFeatureFlagFingerprint.let { it ->
                if (is_20_22_or_greater) {
                    Logger.getLogger(this::class.java.name).severe(
                        "Litho filtering does not yet support 20.22+ Many UI components will not be hidden."
                    )
                }
                it.method.apply {
                    val override = if (is_20_22_or_greater) 0x1 else 0x0
                    val index = indexOfFirstInstructionOrThrow(Opcode.MOVE_RESULT)
                    val register = getInstruction<OneRegisterInstruction>(index).registerA
                    addInstruction(index + 1, "const/4 v$register, $override")
                }
            }

            // endregion
        } else {
            // region Pass the buffer into extension.

            byteBufferFingerprint.methodOrThrow().addInstruction(
                0,
                "invoke-static { p2 }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->setProtoBuffer(Ljava/nio/ByteBuffer;)V"
            )

            // endregion

            // region Hook the method that parses bytes into a ComponentContext.

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
                    it.stringMatches!!.first().index
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
            val emptyComponentField = classBy {
                // Only one field that matches.
                it.type == builderMethodDescriptor.returnType
            }!!.immutableClass.fields.single()

            emptyComponentLabel = """
            move-object/from16 v0, p1
            invoke-static {v0}, $builderMethodDescriptor
            move-result-object v0
            iget-object v0, v0, $emptyComponentField
            return-object v0
            """

            val isLegacyMethod = MethodUtil.methodSignaturesMatch(
                componentContextParserLegacyFingerprint.methodOrThrow(),
                componentContextParserFingerprint2.methodOrThrow()
            )

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
                    invoke-static {v$pathRegister, v$identifierRegister, v$freeRegister}, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->isFiltered(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;)Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :unfiltered
                    """ + emptyComponentLabel + """
                    :unfiltered
                    nop
                    """
                )
            }

            // endregion

            // region Change Litho thread executor to 1 thread to fix layout issue in unpatched YouTube.

            lithoThreadExecutorFingerprint.methodOrThrow().addInstructions(
                0, """
                invoke-static { p1 }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->getExecutorCorePoolSize(I)I
                move-result p1
                invoke-static { p2 }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->getExecutorMaxThreads(I)I
                move-result p2
                """
            )

            // endregion

            // region A/B test of new Litho native code.

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
            val lithoFilterMethods = findMethodsOrThrow(EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR)

            lithoFilterMethods
                .first { it.name == "<clinit>" }
                .apply {
                    val setArrayIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.SPUT_OBJECT &&
                                getReference<FieldReference>()?.type == EXTENSION_FILER_ARRAY_DESCRIPTOR
                    }
                    val setArrayRegister =
                        getInstruction<OneRegisterInstruction>(setArrayIndex).registerA
                    val addedMethodName = "getFilterArray"

                    addInstructions(
                        setArrayIndex, """
                        invoke-static {}, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->$addedMethodName()$EXTENSION_FILER_ARRAY_DESCRIPTOR
                        move-result-object v$setArrayRegister
                        """
                    )

                    filterArrayMethod = ImmutableMethod(
                        definingClass,
                        addedMethodName,
                        emptyList(),
                        EXTENSION_FILER_ARRAY_DESCRIPTOR,
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

    finalize {
        if (isYouTube) {
            // Finalize for YouTube
            lithoFilterFingerprint.method.replaceInstruction(0, "const/16 v0, $filterCount")
        } else {
            // Finalize for YouTube Music
            filterArrayMethod?.addInstructions(
                0, """
                    const/16 v0, $filterCount
                    new-array v2, v0, $EXTENSION_FILER_ARRAY_DESCRIPTOR
                """
            )
        }
    }
}
