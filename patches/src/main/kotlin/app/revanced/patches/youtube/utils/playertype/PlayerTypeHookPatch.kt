package app.revanced.patches.youtube.utils.playertype

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.SHARED_PATH
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.revanced.patches.youtube.utils.resourceid.reelWatchPlayer
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.util.addStaticFieldToExtension
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerTypeHookPatch;"

private const val EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR =
    "$SHARED_PATH/RootView;"

private const val EXTENSION_ROOT_VIEW_TOOLBAR_INTERFACE =
    "$SHARED_PATH/RootView${'$'}AppCompatToolbarPatchInterface;"

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/RelatedVideoFilter;"

val playerTypeHookPatch = bytecodePatch(
    description = "playerTypeHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
    )

    execute {

        // region patch for set player type

        playerTypeFingerprint.methodOrThrow().addInstruction(
            0,
            "invoke-static {p1}, " +
                    "$EXTENSION_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V"
        )

        // endregion

        // region patch for set shorts player state

        reelWatchPagerFingerprint.methodOrThrow().apply {
            val literIndex = indexOfFirstLiteralInstructionOrThrow(reelWatchPlayer) + 2
            val registerIndex = indexOfFirstInstructionOrThrow(literIndex) {
                opcode == Opcode.MOVE_RESULT_OBJECT
            }
            val viewRegister = getInstruction<OneRegisterInstruction>(registerIndex).registerA

            addInstruction(
                registerIndex + 1,
                "invoke-static {v$viewRegister}, " +
                        "$EXTENSION_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->onShortsCreate(Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for set video state

        videoStateFingerprint.matchOrThrow().let {
            it.method.apply {
                val endIndex = it.patternMatch!!.startIndex + 1
                val videoStateFieldName =
                    getInstruction<ReferenceInstruction>(endIndex).reference

                addInstructions(
                    0, """
                        iget-object v0, p1, $videoStateFieldName  # copyvideoState parameter field
                        invoke-static {v0}, $EXTENSION_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->setVideoState(Ljava/lang/Enum;)V
                        """
                )
            }
        }

        // endregion

        // region patch for hook browse id

        browseIdClassFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = indexOfFirstStringInstructionOrThrow("VL") - 1
                val targetClass = getInstruction(targetIndex)
                    .getReference<FieldReference>()
                    ?.definingClass
                    ?: throw PatchException("Could not find browseId class")

                findMethodOrThrow(targetClass).apply {
                    val browseIdFieldReference = getInstruction<ReferenceInstruction>(
                        indexOfFirstInstructionOrThrow(Opcode.IPUT_OBJECT)
                    ).reference
                    val browseIdFieldName = (browseIdFieldReference as FieldReference).name

                    val smaliInstructions =
                        """
                            if-eqz v0, :ignore
                            iget-object v0, v0, $definingClass->$browseIdFieldName:Ljava/lang/String;
                            if-eqz v0, :ignore
                            return-object v0
                            :ignore
                            const-string v0, ""
                            return-object v0
                            """

                    addStaticFieldToExtension(
                        EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR,
                        "getBrowseId",
                        "browseIdClass",
                        definingClass,
                        smaliInstructions
                    )
                }
            }
        }

        // endregion

        // region patch for hook search bar

        searchQueryClassFingerprint.methodOrThrow().apply {
            val searchQueryIndex = indexOfStringIsEmptyInstruction(this) - 1
            val searchQueryFieldReference =
                getInstruction<ReferenceInstruction>(searchQueryIndex).reference
            val searchQueryClass = (searchQueryFieldReference as FieldReference).definingClass

            findMethodOrThrow(searchQueryClass).apply {
                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        iget-object v0, v0, $searchQueryFieldReference
                        if-eqz v0, :ignore
                        return-object v0
                        :ignore
                        const-string v0, ""
                        return-object v0
                        """

                addStaticFieldToExtension(
                    EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR,
                    "getSearchQuery",
                    "searchQueryClass",
                    definingClass,
                    smaliInstructions
                )
            }
        }

        // endregion

        // region patch for hook back button visibility

        toolbarLayoutFingerprint.methodOrThrow().apply {
            val index = indexOfMainCollapsingToolbarLayoutInstruction(this)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstruction(
                index + 1,
                "invoke-static { v$register }, $EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR->setToolbar(Landroid/widget/FrameLayout;)V"
            )
        }

        // Add interface for extensions code to call obfuscated methods.
        appCompatToolbarBackButtonFingerprint.matchOrThrow().let {
            it.classDef.apply {
                interfaces.add(EXTENSION_ROOT_VIEW_TOOLBAR_INTERFACE)

                val definingClass = type
                val obfuscatedMethodName = it.originalMethod.name
                val returnType = "Landroid/graphics/drawable/Drawable;"

                methods.add(
                    ImmutableMethod(
                        definingClass,
                        "patch_getToolbarIcon",
                        listOf(),
                        returnType,
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(2),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                 invoke-virtual { p0 }, $definingClass->$obfuscatedMethodName()$returnType
                                 move-result-object v0
                                 return-object v0
                             """
                        )
                    }
                )
            }
        }

        // endregion

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)
    }
}
