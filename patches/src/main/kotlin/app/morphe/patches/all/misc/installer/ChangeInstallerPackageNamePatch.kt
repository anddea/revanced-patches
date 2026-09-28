package app.morphe.patches.all.misc.installer

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.transformation.IMethodCall
import app.morphe.patches.all.misc.transformation.filterMapInstruction35c
import app.morphe.patches.all.misc.transformation.transformInstructionsPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val GOOGLE_PLAY_STORE_PACKAGE = "com.android.vending"
private const val PACKAGE_INSTALLER_PACKAGE_SOURCE_STORE = 2

/**
 * Replaces both legacy and modern package-manager installer-source results with Google Play.
 *
 * Android exposes the installer through [PackageManager.getInstallerPackageName] on older
 * releases and through [InstallSourceInfo] on newer releases, so all corresponding queries must
 * be patched for app-side store-install checks to see a consistent source.
 */
@Suppress("unused")
val changeInstallerPackageNamePatch = bytecodePatch(
    name = "Change installer package name",
    description = "Spoof the installer package name to make it appear that the app was installed from the Google Play Store.",
    default = false,
) {
    dependsOn(
        // Replace both legacy and modern installer-source queries in the target app.
        transformInstructionsPatch(
            filterMap = { classDef, _, instruction, instructionIndex ->
                filterMapInstruction35c<MethodCall>(
                    "Lapp/morphe/extension",
                    classDef,
                    instruction,
                    instructionIndex,
                )
            },
            transform = transform@{ mutableMethod, entry ->
                val (methodCall, _, instructionIndex) = entry

                mutableMethod.apply {
                    val resultInstruction = getInstruction(instructionIndex + 1)
                    val expectedResultOpcode = if (methodCall.returnType == "I") {
                        Opcode.MOVE_RESULT
                    } else {
                        Opcode.MOVE_RESULT_OBJECT
                    }
                    if (resultInstruction.opcode != expectedResultOpcode) return@transform

                    val targetRegister = (resultInstruction as? OneRegisterInstruction
                        ?: return@transform).registerA
                    val replacement = if (methodCall.returnType == "I") {
                        "const/4 v$targetRegister, 0x${PACKAGE_INSTALLER_PACKAGE_SOURCE_STORE.toString(16)}"
                    } else {
                        "const-string v$targetRegister, \"$GOOGLE_PLAY_STORE_PACKAGE\""
                    }

                    replaceInstruction(
                        instructionIndex + 1,
                        replacement,
                    )
                    replaceInstruction(
                        instructionIndex,
                        replacement,
                    )
                }
            },
        ),
    )
}

// Information about method calls we want to replace
@Suppress("unused")
private enum class MethodCall(
    override val definedClassName: String,
    override val methodName: String,
    override val methodParams: Array<String>,
    override val returnType: String,
) : IMethodCall {
    GetInstallerPackageName(
        "Landroid/content/pm/PackageManager;",
        "getInstallerPackageName",
        arrayOf("Ljava/lang/String;"),
        "Ljava/lang/String;",
    ),
    GetInstallingPackageName(
        "Landroid/content/pm/InstallSourceInfo;",
        "getInstallingPackageName",
        arrayOf(),
        "Ljava/lang/String;",
    ),
    GetInitiatingPackageName(
        "Landroid/content/pm/InstallSourceInfo;",
        "getInitiatingPackageName",
        arrayOf(),
        "Ljava/lang/String;",
    ),
    GetPackageSource(
        "Landroid/content/pm/InstallSourceInfo;",
        "getPackageSource",
        arrayOf(),
        "I",
    ),
}
