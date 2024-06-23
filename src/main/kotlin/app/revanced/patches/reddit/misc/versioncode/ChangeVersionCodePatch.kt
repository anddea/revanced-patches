package app.revanced.patches.reddit.misc.versioncode

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.intPatchOption
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.patch.BaseResourcePatch
import app.revanced.util.valueOrThrow
import org.w3c.dom.Element

@Suppress("unused")
object ChangeVersionCodePatch : BaseResourcePatch(
    name = "Change version code",
    description = "Changes the version code of the app. By default the highest version code is set. " +
            "This allows older versions of an app to be installed " +
            "if their version code is set to the same or a higher value and can stop app stores to update the app.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    private val ChangeVersionCode by booleanPatchOption(
        key = "ChangeVersionCode",
        default = false,
        title = "Change version code",
        description = "Changes the version code of the app.",
        required = true
    )

    private val VersionCode = intPatchOption(
        key = "VersionCode",
        default = Int.MAX_VALUE,
        title = "Version code",
        description = "The version code to use.",
        required = true
    )

    override fun execute(context: ResourceContext) {
        if (ChangeVersionCode == false) {
            println("INFO: Version code will remain unchanged as 'ChangeVersionCode' is false.")
            return
        }

        val versionCode = VersionCode.valueOrThrow()

        if (versionCode < 1) {
            throw PatchException(
                "Invalid versionCode: $versionCode, " +
                        "Version code should be larger than 1 and smaller than ${Int.MAX_VALUE}."
            )
        }

        context.document["AndroidManifest.xml"].use { document ->
            val manifestElement = document.getElementsByTagName("manifest").item(0) as Element
            manifestElement.setAttribute("android:versionCode", "$versionCode")
        }
    }
}
