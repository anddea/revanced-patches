package app.revanced.patches.reddit.layout.recentlyvisited

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.reddit.layout.recentlyvisited.fingerprints.CommunityDrawerPresenterFingerprint
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getTargetIndexWithFieldReferenceName
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Suppress("unused")
object RecentlyVisitedShelfPatch : BaseBytecodePatch(
    name = "Hide recently visited shelf",
    description = "Adds an option to hide the recently visited shelf in the sidebar.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(CommunityDrawerPresenterFingerprint)
) {
    private const val INTEGRATIONS_METHOD_DESCRIPTOR =
        "$PATCHES_PATH/RecentlyVisitedShelfPatch;" +
                "->" +
                "hideRecentlyVisitedShelf(Ljava/util/List;)Ljava/util/List;"

    override fun execute(context: BytecodeContext) {

        CommunityDrawerPresenterFingerprint.resultOrThrow().let {
            lateinit var recentlyVisitedReference: Reference

            it.mutableClass.methods.find { method -> method.name == "<init>" }
                ?.apply {
                    val recentlyVisitedFieldIndex =
                        getTargetIndexWithFieldReferenceName("RECENTLY_VISITED")
                    val recentlyVisitedObjectIndex =
                        getTargetIndex(recentlyVisitedFieldIndex, Opcode.IPUT_OBJECT)
                    recentlyVisitedReference =
                        getInstruction<ReferenceInstruction>(recentlyVisitedObjectIndex).reference
                } ?: throw PatchException("Constructor method not found!")

            it.mutableMethod.apply {
                val recentlyVisitedObjectIndex =
                    getTargetIndexWithReference(recentlyVisitedReference.toString())
                arrayOf(
                    getTargetIndex(recentlyVisitedObjectIndex, Opcode.INVOKE_STATIC),
                    getTargetIndexReversed(recentlyVisitedObjectIndex, Opcode.INVOKE_STATIC)
                ).forEach { staticIndex ->
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(staticIndex + 1).registerA

                    addInstructions(
                        staticIndex + 2, """
                            invoke-static {v$insertRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR
                            move-result-object v$insertRegister
                            """
                    )
                }
            }
        }

        updateSettingsStatus("enableRecentlyVisitedShelf")

    }
}
