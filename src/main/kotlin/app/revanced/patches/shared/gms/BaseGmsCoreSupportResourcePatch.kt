package app.revanced.patches.shared.gms

import app.revanced.patcher.PatchClass
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.shared.packagename.PackageNamePatch
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Abstract resource patch that allows Google apps to run without root and under a different package name
 * by using GmsCore instead of Google Play Services.
 *
 * @param fromPackageName The package name of the original app.
 * @param spoofedPackageSignature The signature of the package to spoof to.
 * @param dependencies Additional dependencies of this patch.
 */
@Suppress("DEPRECATION")
abstract class BaseGmsCoreSupportResourcePatch(
    private val fromPackageName: String,
    private val spoofedPackageSignature: String,
    dependencies: Set<PatchClass> = setOf(),
) : ResourcePatch(
    dependencies = setOf(PackageNamePatch::class) + dependencies
) {
    private val gmsCoreVendorGroupId = "app.revanced"

    override fun execute(context: ResourceContext) {
        context.patchManifest()
        context.addSpoofingMetadata()
    }

    /**
     * Add metadata to manifest to support spoofing the package name and signature of GmsCore.
     */
    private fun ResourceContext.addSpoofingMetadata() {
        fun Node.adoptChild(
            tagName: String,
            block: Element.() -> Unit,
        ) {
            val child = ownerDocument.createElement(tagName)
            child.block()
            appendChild(child)
        }

        xmlEditor["AndroidManifest.xml"].use { editor ->
            val document = editor.file

            val applicationNode =
                document
                    .getElementsByTagName("application")
                    .item(0)

            // Spoof package name and signature.
            applicationNode.adoptChild("meta-data") {
                setAttribute("android:name", "$gmsCoreVendorGroupId.android.gms.SPOOFED_PACKAGE_NAME")
                setAttribute("android:value", fromPackageName)
            }

            applicationNode.adoptChild("meta-data") {
                setAttribute("android:name", "$gmsCoreVendorGroupId.android.gms.SPOOFED_PACKAGE_SIGNATURE")
                setAttribute("android:value", spoofedPackageSignature)
            }

            // GmsCore presence detection in ReVanced Integrations.
            applicationNode.adoptChild("meta-data") {
                // TODO: The name of this metadata should be dynamic.
                setAttribute("android:name", "app.revanced.MICROG_PACKAGE_NAME")
                setAttribute("android:value", "$gmsCoreVendorGroupId.android.gms")
            }
        }
    }

    /**
     * Patch the manifest to support GmsCore.
     */
    private fun ResourceContext.patchManifest() {
        val packageName = PackageNamePatch.getPackageName(fromPackageName)

        val manifest = this["AndroidManifest.xml"].readText()
        this["AndroidManifest.xml"].writeText(
            manifest.replace(
                "package=\"$fromPackageName",
                "package=\"$packageName",
            ).replace(
                "android:authorities=\"$fromPackageName",
                "android:authorities=\"$packageName",
            ).replace(
                "$fromPackageName.permission.C2D_MESSAGE",
                "$packageName.permission.C2D_MESSAGE",
            ).replace(
                "$fromPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                "$packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            ).replace(
                "com.google.android.c2dm",
                "$gmsCoreVendorGroupId.android.c2dm",
            ).replace(
                "</queries>",
                "<package android:name=\"$gmsCoreVendorGroupId.android.gms\"/></queries>",
            ),
        )
    }
}
