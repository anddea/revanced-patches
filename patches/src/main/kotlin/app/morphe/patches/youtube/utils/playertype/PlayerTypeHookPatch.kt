package app.morphe.patches.youtube.utils.playertype

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.SHARED_PATH
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.resourceid.reelWatchPlayer
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.addStaticFieldToExtension
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerTypeHookPatch;"

private const val EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR =
    "$SHARED_PATH/RootView;"

private const val EXTENSION_ROOT_VIEW_TOOLBAR_INTERFACE =
    "$SHARED_PATH/RootView${'$'}AppCompatToolbarPatchInterface;"

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/LayoutReloadObserverFilter;"

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

        // region patch for set ad progress text visibility

        adProgressTextViewVisibilityFingerprint.methodOrThrow().apply {
            val index =
                indexOfAdProgressTextViewVisibilityInstruction(this)
            val register =
                getInstruction<FiveRegisterInstruction>(index).registerD

            addInstructionsAtControlFlowLabel(
                index,
                "invoke-static { v$register }, " +
                        EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR +
                        "->setAdProgressTextVisibility(I)V"
            )
        }

        // endregion

        // region patch for set context

        componentHostFingerprint.methodOrThrow().apply {
            val index = indexOfGetContextInstruction(this)
            val register =
                getInstruction<TwoRegisterInstruction>(index).registerA

            addInstructionsAtControlFlowLabel(
                index + 1,
                "invoke-static { v$register }, " +
                        EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR +
                        "->setContext(Landroid/content/Context;)V"
            )
        }

        // endregion

        // region patch for set player type

        val PlayerOverlaysSetPlayerTypeFingerprint = Fingerprint(
            accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
            returnType = "V",
            parameters = listOf(PlayerTypeEnumFingerprint.originalClassDef.type),
            custom = { _, classDef ->
                classDef.endsWith("/YouTubePlayerOverlaysLayout;")
            }
        )

        PlayerOverlaysSetPlayerTypeFingerprint.method.addInstruction(
            0,
            "invoke-static { p1 }, $EXTENSION_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V",
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
                val endIndex = it.instructionMatches.first().index + 1
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
