package app.revanced.patches.youtube.navigation.shortsnavbar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.navigation.shortsnavbar.fingerprints.NavigationEndpointFingerprint
import app.revanced.patches.youtube.navigation.shortsnavbar.fingerprints.ReelWatchBundleFingerprint
import app.revanced.patches.youtube.navigation.shortsnavbar.fingerprints.ReelWatchEndpointFingerprint
import app.revanced.patches.youtube.navigation.shortsnavbar.fingerprints.ReelWatchEndpointParentFingerprint
import app.revanced.patches.youtube.navigation.shortsnavbar.fingerprints.SetPivotBarFingerprint
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference

@Patch
@Name("hide-shorts-navbar")
@Description("Hide navigation bar when playing shorts.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ShortsNavBarPatch : BytecodePatch(
    listOf(
        NavigationEndpointFingerprint,
        PivotBarCreateButtonViewFingerprint,
        ReelWatchBundleFingerprint,
        ReelWatchEndpointParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PivotBarCreateButtonViewFingerprint.result?.let { parentResult ->
            SetPivotBarFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val register = getInstruction<OneRegisterInstruction>(startIndex).registerA

                    addInstruction(
                        startIndex + 1,
                        "sput-object v$register, $NAVIGATION->pivotBar:Ljava/lang/Object;"
                    )
                }
            } ?: return SetPivotBarFingerprint.toErrorResult()
        } ?: return PivotBarCreateButtonViewFingerprint.toErrorResult()

        ReelWatchBundleFingerprint.result?.let {
            (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod
            ).apply {
                addInstruction(
                    0,
                    "invoke-static {}, $NAVIGATION->hideShortsPlayerNavBar()V"
                )
            }
        } ?: return ReelWatchBundleFingerprint.toErrorResult()

        ReelWatchEndpointParentFingerprint.result?.let { parentResult ->
            ReelWatchEndpointFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.addInstruction(
                0,
                "sput-object p1, $NAVIGATION->shortsContext:Landroid/content/Context;"
            ) ?: return ReelWatchEndpointFingerprint.toErrorResult()
        } ?: return ReelWatchEndpointParentFingerprint.toErrorResult()

        NavigationEndpointFingerprint.result?.let { result ->
            val navigationEndpointMethod = result.mutableMethod

            with (navigationEndpointMethod.implementation!!.instructions) {
                filter { instruction ->
                    val fieldReference =
                        (instruction as? ReferenceInstruction)?.reference as? FieldReference
                    fieldReference?.let { it.type == "Lcom/google/android/apps/youtube/app/extensions/reel/watch/player/ReelObscuredPlaybackSuspender;" } == true
                }.forEach { instruction ->
                    val insertIndex = indexOf(instruction) + 4
                    val targetRegister =
                        navigationEndpointMethod.getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    navigationEndpointMethod.addInstructions(
                        insertIndex,
                        """
                            invoke-static {v$targetRegister}, $NAVIGATION->hideShortsPlayerNavBar(Landroid/view/View;)Landroid/view/View;
                            move-result-object v$targetRegister
                        """
                    )
                }
            }

        } ?: return NavigationEndpointFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_SHORTS_NAVIGATION_BAR"
            )
        )

        SettingsPatch.updatePatchStatus("hide-shorts-navbar")

        return PatchResultSuccess()
    }
}