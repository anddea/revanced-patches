package app.revanced.patches.shared.litho

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.playservice.is_19_25_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_05_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_20_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_22_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.util.*
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
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
        isYouTube = conversionContextFingerprintToString.originalClassDefOrNull != null
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

            protobufBufferReferenceFingerprint.method.addInstruction(
                0,
                "invoke-static { p2 }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->setProtoBuffer(Ljava/nio/ByteBuffer;)V",
            )

            // endregion

            // region Hook the method that parses bytes into a ComponentContext.

            // 20.20+ has combined the two methods together,
            // and the sub parser fingerprint identifies the method to patch.
            val contextParserMethodToModifyFingerprint =
                if (is_20_20_or_greater) componentContextSubParserFingerprint
                else componentContextParserFingerprint

            // Allow the method to run to completion, and override the
            // return value with an empty component if it should be filtered.
            // It is important to allow the original code to always run to completion,
            // otherwise memory leaks and poor app performance can occur.
            //
            // The extension filtering result needs to be saved off somewhere, but cannot
            // save to a class field since the target class is called by multiple threads.
            // It would be great if there was a way to change the register count of the
            // method implementation and save the result to a high register to later use
            // in the method, but there is no simple way to do that.
            // Instead, save the extension filter result to a thread local and check the
            // filtering result at each method return index.
            // String field for the litho identifier.
            contextParserMethodToModifyFingerprint.method.apply {
                val conversionContextClass = conversionContextFingerprintToString.originalClassDef

                val conversionContextIdentifierField = componentContextSubParserFingerprint.match(
                    contextParserMethodToModifyFingerprint.originalClassDef
                ).let {
                    // Identifier field is loaded just before the string declaration.
                    val index = it.method.indexOfFirstInstructionReversedOrThrow(
                        it.stringMatches!!.first().index
                    ) {
                        val reference = getReference<FieldReference>()
                        reference?.definingClass == conversionContextClass.type
                                && reference.type == "Ljava/lang/String;"
                    }
                    it.method.getInstruction<ReferenceInstruction>(index).getReference<FieldReference>()
                }

                // StringBuilder field for the litho path.
                val conversionContextPathBuilderField = conversionContextClass.fields
                    .single { field -> field.type == "Ljava/lang/StringBuilder;" }

                val conversionContextResultIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    reference?.returnType == conversionContextClass.type
                } + 1

                val conversionContextResultRegister = getInstruction<OneRegisterInstruction>(
                    conversionContextResultIndex
                ).registerA

                val identifierRegister = findFreeRegister(
                    conversionContextResultIndex, conversionContextResultRegister
                )
                val stringBuilderRegister = findFreeRegister(
                    conversionContextResultIndex, conversionContextResultRegister, identifierRegister
                )

                // Check if the component should be filtered, and save the result to a thread local.
                addInstructionsAtControlFlowLabel(
                    conversionContextResultIndex + 1,
                    """
                        iget-object v$identifierRegister, v$conversionContextResultRegister, $conversionContextIdentifierField
                        iget-object v$stringBuilderRegister, v$conversionContextResultRegister, $conversionContextPathBuilderField
                        invoke-static { v$identifierRegister, v$stringBuilderRegister, v$conversionContextResultRegister }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->filter(Ljava/lang/String;Ljava/lang/StringBuilder;Ljava/lang/Object;)V
                    """
                )

                // Get the only static method in the class.
                val builderMethodDescriptor = emptyComponentFingerprint.classDef.methods.single { method ->
                    AccessFlags.STATIC.isSet(method.accessFlags)
                }
                // Only one field.
                val emptyComponentField = classBy { classDef ->
                    classDef.type == builderMethodDescriptor.returnType
                }!!.immutableClass.fields.single()

                // Check at each return value if the component is filtered,
                // and return an empty component if filtering is needed.
                findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { returnIndex ->
                    val freeRegister = findFreeRegister(returnIndex)

                    emptyComponentLabel = """
                        invoke-static { }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->shouldFilter()Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :unfiltered
        
                        move-object/from16 v$freeRegister, p1
                        invoke-static { v$freeRegister }, $builderMethodDescriptor
                        move-result-object v$freeRegister
                        iget-object v$freeRegister, v$freeRegister, $emptyComponentField
                        return-object v$freeRegister
        
                        :unfiltered
                        nop    
                    """

                    addInstructionsAtControlFlowLabel(
                        returnIndex, emptyComponentLabel
                    )
                }
            }

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
            // If this is enabled, then the litho protobuf hook will always show an empty buffer
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

            var (emptyComponentMethod, emptyComponentLabel) =
                emptyComponentsFingerprint.matchOrThrow().let {
                    with(it.method) {
                        val emptyComponentMethodIndex = it.patternMatch!!.startIndex + 1
                        val emptyComponentMethodReference =
                            getInstruction<ReferenceInstruction>(emptyComponentMethodIndex).reference
                        val emptyComponentFieldReference =
                            getInstruction<ReferenceInstruction>(emptyComponentMethodIndex + 2).reference

                        val label = """
                        move-object/from16 v0, p1
                        invoke-static {v0}, $emptyComponentMethodReference
                        move-result-object v0
                        iget-object v0, v0, $emptyComponentFieldReference
                        return-object v0
                        """

                        emptyComponentLabel = label

                        Pair(this, label)
                    }
                }

            fun checkMethodSignatureMatch(pathBuilder: MutableMethod) = emptyComponentMethod.apply {
                if (!MethodUtil.methodSignaturesMatch(pathBuilder, this)) {
                    implementation!!.instructions
                        .withIndex()
                        .filter { (_, instruction) ->
                            val reference = (instruction as? ReferenceInstruction)?.reference
                            reference is MethodReference &&
                                    MethodUtil.methodSignaturesMatch(pathBuilder, reference)
                        }
                        .map { (index, _) -> index }
                        .reversed()
                        .forEach { index ->
                            val insertInstruction = getInstruction(index + 1)
                            if (insertInstruction is OneRegisterInstruction) {
                                val insertRegister =
                                    insertInstruction.registerA
                                val insertIndex = index + 2

                                addInstructionsWithLabels(
                                    insertIndex, """
                                    if-nez v$insertRegister, :ignore
                                    """ + emptyComponentLabel,
                                    ExternalLabel("ignore", getInstruction(insertIndex))
                                )
                            }
                        }

                    emptyComponentLabel = """
                    const/4 v0, 0x0
                    return-object v0
                    """
                }
            }

            pathBuilderFingerprint.methodOrThrow().apply {
                checkMethodSignatureMatch(this)

                val stringBuilderIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.type == "Ljava/lang/StringBuilder;"
                }
                val stringBuilderRegister =
                    getInstruction<TwoRegisterInstruction>(stringBuilderIndex).registerA

                val emptyStringIndex = indexOfFirstStringInstruction("")
                val relativeIndex = if (emptyStringIndex > -1) {
                    emptyStringIndex
                } else {
                    val separatorIndex = indexOfFirstStringInstructionOrThrow("|")
                    indexOfFirstInstructionOrThrow(separatorIndex) {
                        opcode == Opcode.NEW_INSTANCE &&
                                getReference<TypeReference>()?.type == "Ljava/lang/StringBuilder;"
                    }
                }

                val identifierRegister = getInstruction<TwoRegisterInstruction>(
                    indexOfFirstInstructionReversedOrThrow(relativeIndex) {
                        opcode == Opcode.IPUT_OBJECT
                                && getReference<FieldReference>()?.type == "Ljava/lang/String;"
                    }
                ).registerA
                val objectRegister = getInstruction<FiveRegisterInstruction>(
                    indexOfFirstInstructionOrThrow(relativeIndex, Opcode.INVOKE_VIRTUAL)
                ).registerC

                val insertIndex = stringBuilderIndex + 1

                addInstructionsWithLabels(
                    insertIndex, """
                    invoke-static { v$identifierRegister, v$stringBuilderRegister, v$objectRegister }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->filter(Ljava/lang/String;Ljava/lang/StringBuilder;Ljava/lang/Object;)V
                    invoke-static {}, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->shouldFilter()Z
                    move-result v$stringBuilderRegister
                    if-eqz v$stringBuilderRegister, :filter
                    """ + emptyComponentLabel,
                    ExternalLabel("filter", getInstruction(insertIndex))
                )
            }

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
