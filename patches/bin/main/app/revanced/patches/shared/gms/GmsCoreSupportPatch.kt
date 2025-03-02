package app.revanced.patches.shared.gms

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatchBuilder
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.Option
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatchBuilder
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.patches.shared.gms.Constants.ACTIONS
import app.revanced.patches.shared.gms.Constants.AUTHORITIES
import app.revanced.patches.shared.gms.Constants.PERMISSIONS
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.returnEarly
import app.revanced.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element
import org.w3c.dom.Node

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/GmsCoreSupport;"

private const val PACKAGE_NAME_REGEX_PATTERN = "^[a-z]\\w*(\\.[a-z]\\w*)+\$"

private const val CLONE_PACKAGE_NAME_YOUTUBE = "bill.youtube"
private const val DEFAULT_PACKAGE_NAME_YOUTUBE = "anddea.youtube"
internal const val ORIGINAL_PACKAGE_NAME_YOUTUBE = "com.google.android.youtube"

private const val CLONE_PACKAGE_NAME_YOUTUBE_MUSIC = "bill.youtube.music"
private const val DEFAULT_PACKAGE_NAME_YOUTUBE_MUSIC = "anddea.youtube.music"
internal const val ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC =
    "com.google.android.apps.youtube.music"

/**
 * A patch that allows patched Google apps to run without root and under a different package name
 * by using GmsCore instead of Google Play Services.
 *
 * @param fromPackageName The package name of the original app.
 * @param mainActivityOnCreateFingerprint The fingerprint of the main activity onCreate method.
 * @param extensionPatch The patch responsible for the extension.
 * @param gmsCoreSupportResourcePatchFactory The factory for the corresponding resource patch
 * that is used to patch the resources.
 * @param executeBlock The additional execution block of the patch.
 * @param block The additional block to build the patch.
 */
fun gmsCoreSupportPatch(
    fromPackageName: String,
    mainActivityOnCreateFingerprint: Fingerprint,
    extensionPatch: Patch<*>,
    gmsCoreSupportResourcePatchFactory: (gmsCoreVendorGroupIdOption: Option<String>, packageNameYouTubeOption: Option<String>, packageNameYouTubeMusicOption: Option<String>) -> Patch<*>,
    executeBlock: BytecodePatchContext.() -> Unit = {},
    block: BytecodePatchBuilder.() -> Unit = {},
) = bytecodePatch(
    name = "GmsCore support",
    description = "Allows patched Google apps to run without root and under a different package name " +
            "by using GmsCore instead of Google Play Services.",
) {
    val gmsCoreVendorGroupIdOption = stringOption(
        key = "gmsCoreVendorGroupId",
        default = "app.revanced",
        values =
        mapOf(
            "ReVanced" to "app.revanced",
        ),
        title = "GmsCore vendor group ID",
        description = "The vendor's group ID for GmsCore.",
        required = true,
    ) { it!!.matches(Regex(PACKAGE_NAME_REGEX_PATTERN)) }

    val checkGmsCore by booleanOption(
        key = "checkGmsCore",
        default = true,
        title = "Check GmsCore",
        description = """
            Check if GmsCore is installed on the device and has battery optimizations disabled when the app starts. 
            
            If GmsCore is not installed the app will not work, so disabling this is not recommended.
            """.trimIndentMultiline(),
        required = true,
    )

    val packageNameYouTubeOption = stringOption(
        key = "packageNameYouTube",
        default = DEFAULT_PACKAGE_NAME_YOUTUBE,
        values = mapOf(
            "Clone" to CLONE_PACKAGE_NAME_YOUTUBE,
            "Default" to DEFAULT_PACKAGE_NAME_YOUTUBE
        ),
        title = "Package name of YouTube",
        description = "The name of the package to use in GmsCore support.",
        required = true
    ) { it!!.matches(Regex(PACKAGE_NAME_REGEX_PATTERN)) && it != ORIGINAL_PACKAGE_NAME_YOUTUBE }

    val packageNameYouTubeMusicOption = stringOption(
        key = "packageNameYouTubeMusic",
        default = DEFAULT_PACKAGE_NAME_YOUTUBE_MUSIC,
        values = mapOf(
            "Clone" to CLONE_PACKAGE_NAME_YOUTUBE_MUSIC,
            "Default" to DEFAULT_PACKAGE_NAME_YOUTUBE_MUSIC
        ),
        title = "Package name of YouTube Music",
        description = "The name of the package to use in GmsCore support.",
        required = true
    ) { it!!.matches(Regex(PACKAGE_NAME_REGEX_PATTERN)) && it != ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC }

    dependsOn(
        gmsCoreSupportResourcePatchFactory(
            gmsCoreVendorGroupIdOption,
            packageNameYouTubeOption,
            packageNameYouTubeMusicOption
        ),
        extensionPatch,
    )

    val gmsCoreVendorGroupId by gmsCoreVendorGroupIdOption

    execute {
        fun transformStringReferences(transform: (str: String) -> String?) = classes.forEach {
            val mutableClass by lazy {
                proxy(it).mutableClass
            }

            it.methods.forEach classLoop@{ method ->
                val implementation = method.implementation ?: return@classLoop

                val mutableMethod by lazy {
                    mutableClass.methods.first { target ->
                        MethodUtil.methodSignaturesMatch(
                            target,
                            method
                        )
                    }
                }

                implementation.instructions.forEachIndexed insnLoop@{ index, instruction ->
                    val string =
                        ((instruction as? Instruction21c)?.reference as? StringReference)?.string
                            ?: return@insnLoop

                    // Apply transformation.
                    val transformedString = transform(string) ?: return@insnLoop

                    mutableMethod.replaceInstruction(
                        index,
                        BuilderInstruction21c(
                            Opcode.CONST_STRING,
                            instruction.registerA,
                            ImmutableStringReference(transformedString),
                        ),
                    )
                }
            }
        }

        // region Collection of transformations that are applied to all strings.

        fun commonTransform(referencedString: String): String? =
            when (referencedString) {
                "com.google",
                "com.google.android.gms",
                in PERMISSIONS,
                in ACTIONS,
                in AUTHORITIES,
                    -> referencedString.replace("com.google", gmsCoreVendorGroupId!!)

                // TODO: Add this permission when bumping GmsCore
                // "android.media.MediaRouteProviderService" -> "$gmsCoreVendorGroupId.android.media.MediaRouteProviderService"
                else -> null
            }

        fun contentUrisTransform(str: String): String? {
            // only when content:// uri
            if (str.startsWith("content://")) {
                // check if matches any authority
                for (authority in AUTHORITIES) {
                    val uriPrefix = "content://$authority"
                    if (str.startsWith(uriPrefix)) {
                        return str.replace(
                            uriPrefix,
                            "content://${authority.replace("com.google", gmsCoreVendorGroupId!!)}",
                        )
                    }
                }
            }

            return null
        }

        fun packageNameTransform(
            fromPackageName: String,
            toPackageName: String
        ): (String) -> String? = { string ->
            when (string) {
                "$fromPackageName.SuggestionsProvider",
                "$fromPackageName.fileprovider",
                    -> string.replace(fromPackageName, toPackageName)

                else -> null
            }
        }

        fun transformPrimeMethod() {
            setOf(
                primesBackgroundInitializationFingerprint,
                primesLifecycleEventFingerprint
            ).forEach { fingerprint ->
                fingerprint.methodOrThrow().apply {
                    val exceptionIndex = indexOfFirstInstructionReversedOrThrow {
                        opcode == Opcode.NEW_INSTANCE &&
                                (this as? ReferenceInstruction)?.reference?.toString() == "Ljava/lang/IllegalStateException;"
                    }
                    val index =
                        indexOfFirstInstructionReversedOrThrow(exceptionIndex, Opcode.IF_EQZ)
                    val register = getInstruction<OneRegisterInstruction>(index).registerA
                    addInstruction(
                        index,
                        "const/4 v$register, 0x1"
                    )
                }
            }
            primesApiFingerprint.mutableClassOrThrow().methods.filter { method ->
                method.name != "<clinit>" &&
                        method.returnType == "V"
            }.forEach { method ->
                method.apply {
                    val index = if (MethodUtil.isConstructor(method))
                        indexOfFirstInstructionOrThrow {
                            opcode == Opcode.INVOKE_DIRECT &&
                                    getReference<MethodReference>()?.name == "<init>"
                        } + 1
                    else 0
                    addInstruction(
                        index,
                        "return-void"
                    )
                }
            }
        }

        // endregion

        val packageName =
            getPackageName(fromPackageName, packageNameYouTubeOption, packageNameYouTubeMusicOption)

        // Transform all strings using all provided transforms, first match wins.
        val transformations = arrayOf(
            ::commonTransform,
            ::contentUrisTransform,
            packageNameTransform(fromPackageName, packageName),
        )
        transformStringReferences transform@{ string ->
            transformations.forEach { transform ->
                transform(string)?.let { transformedString -> return@transform transformedString }
            }

            return@transform null
        }

        // Return these methods early to prevent the app from crashing.
        setOf(
            castContextFetchFingerprint,
            castDynamiteModuleFingerprint,
            castDynamiteModuleV2Fingerprint,
            googlePlayUtilityFingerprint,
            serviceCheckFingerprint,
            sslGuardFingerprint,
        ).forEach { it.methodOrThrow().returnEarly() }

        // Prevent spam logs.
        eCatcherFingerprint.methodOrThrow().apply {
            val index = indexOfFirstInstructionOrThrow(Opcode.NEW_ARRAY)
            addInstruction(index, "return-void")
        }

        // Specific method that needs to be patched.
        transformPrimeMethod()

        // Verify GmsCore is installed and whitelisted for power optimizations and background usage.
        mainActivityOnCreateFingerprint.method.apply {
            // Temporary fix for patches with an extension patch that hook the onCreate method as well.
            val setContextIndex = indexOfFirstInstruction {
                val reference =
                    getReference<MethodReference>() ?: return@indexOfFirstInstruction false

                reference.toString() == "Lapp/revanced/extension/shared/Utils;->setContext(Landroid/content/Context;)V"
            }

            // Add after setContext call, because this patch needs the context.
            if (checkGmsCore == true) {
                addInstructions(
                    if (setContextIndex < 0) 0 else setContextIndex + 1,
                    "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                            "checkGmsCore(Landroid/app/Activity;)V",
                )
            }
        }

        // Change the vendor of GmsCore in the extension.
        gmsCoreSupportFingerprint.mutableClassOrThrow().methods
            .single { it.name == GET_GMS_CORE_VENDOR_GROUP_ID_METHOD_NAME }
            .replaceInstruction(0, "const-string v0, \"$gmsCoreVendorGroupId\"")

        executeBlock()
    }

    block()
}

/**
 * A collection of permissions, intents and content provider authorities
 * that are present in GmsCore which need to be transformed.
 */
private object Constants {
    val PERMISSIONS = setOf(
        // C2DM / GCM
        "com.google.android.c2dm.permission.RECEIVE",
        "com.google.android.c2dm.permission.SEND",
        "com.google.android.providers.gsf.permission.READ_GSERVICES",

        // ads
        "com.google.android.gms.permission.AD_ID",
        "com.google.android.gms.permission.AD_ID_NOTIFICATION",

        // TODO: Add this permission when bumping GmsCore
        // "com.google.android.gms.permission.ACTIVITY_RECOGNITION",
    )

    val ACTIONS = setOf(
        // C2DM / GCM
        "com.google.android.c2dm.intent.REGISTER",
        "com.google.android.c2dm.intent.REGISTRATION",
        "com.google.android.c2dm.intent.RECEIVE",
        "com.google.iid.TOKEN_REQUEST",

        // people
        "com.google.android.gms.people.service.START",

        // auth
        "com.google.android.gsf.login",
        "com.google.android.gsf.action.GET_GLS",
        "com.google.android.gms.auth.service.START",
        "com.google.android.gms.signin.service.START",
        "com.google.android.gms.accountsettings.action.VIEW_SETTINGS",
        "com.google.android.gms.auth.account.authapi.START",

        // gass
        "com.google.android.gms.gass.START",

        // help
        "com.google.android.gms.googlehelp.service.GoogleHelpService.START",
        "com.google.android.gms.googlehelp.HELP",
        "com.google.android.gms.feedback.internal.IFeedbackService",

        // cast
        "com.google.android.gms.cast.firstparty.START",
        "com.google.android.gms.cast.service.BIND_CAST_DEVICE_CONTROLLER_SERVICE",

        // TODO: Add this permission when bumping GmsCore
        // "android.media.MediaRouteProviderService",

        // fonts
        "com.google.android.gms.fonts",

        // phenotype
        "com.google.android.gms.phenotype.service.START",

        // misc
        "com.google.android.gms.ads.identifier.service.START",
        "com.google.android.gms.clearcut.service.START",
        "com.google.android.gms.common.telemetry.service.START",
        "com.google.android.gms.gmscompliance.service.START",
        "com.google.android.gms.icing.LIGHTWEIGHT_INDEX_SERVICE",
        "com.google.android.gms.languageprofile.service.START",
        "com.google.android.gms.measurement.START",
        "com.google.android.gms.pseudonymous.service.START",
        "com.google.android.gms.usagereporting.service.START",
        "com.google.android.gms.wallet.service.BIND",

        // potoken
        "com.google.android.gms.potokens.service.START",

        // droidguard
        "com.google.android.gms.droidguard.service.START",
    )

    val AUTHORITIES = setOf(
        // gsf
        "com.google.android.gsf.gservices",

        // auth
        "com.google.android.gms.auth.accounts",

        // fonts
        "com.google.android.gms.fonts",

        // phenotype
        "com.google.android.gms.phenotype",
    )
}

private fun getPackageName(
    originalPackageName: String,
    packageNameYouTubeOption: Option<String>,
    packageNameYouTubeMusicOption: Option<String>
): String {
    if (originalPackageName == ORIGINAL_PACKAGE_NAME_YOUTUBE) {
        return packageNameYouTubeOption.valueOrThrow()
    } else if (originalPackageName == ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC) {
        return packageNameYouTubeMusicOption.valueOrThrow()
    }
    throw PatchException("Unknown package name: $originalPackageName")
}

/**
 * Abstract resource patch that allows Google apps to run without root and under a different package name
 * by using GmsCore instead of Google Play Services.
 *
 * @param fromPackageName The package name of the original app.
 * @param spoofedPackageSignature The signature of the package to spoof to.
 * @param gmsCoreVendorGroupIdOption The option to get the vendor group ID of GmsCore.
 * @param executeBlock The additional execution block of the patch.
 * @param block The additional block to build the patch.
 */
fun gmsCoreSupportResourcePatch(
    fromPackageName: String,
    spoofedPackageSignature: String,
    gmsCoreVendorGroupIdOption: Option<String>,
    packageNameYouTubeOption: Option<String>,
    packageNameYouTubeMusicOption: Option<String>,
    executeBlock: ResourcePatchContext.() -> Unit = {},
    block: ResourcePatchBuilder.() -> Unit = {},
) = resourcePatch {
    val gmsCoreVendorGroupId by gmsCoreVendorGroupIdOption

    execute {
        /**
         * Add metadata to manifest to support spoofing the package name and signature of GmsCore.
         */
        fun addSpoofingMetadata() {
            fun Node.adoptChild(
                tagName: String,
                block: Element.() -> Unit,
            ) {
                val child = ownerDocument.createElement(tagName)
                child.block()
                appendChild(child)
            }

            document("AndroidManifest.xml").use { document ->
                val applicationNode =
                    document
                        .getElementsByTagName("application")
                        .item(0)

                // Spoof package name and signature.
                applicationNode.adoptChild("meta-data") {
                    setAttribute(
                        "android:name",
                        "$gmsCoreVendorGroupId.android.gms.SPOOFED_PACKAGE_NAME"
                    )
                    setAttribute("android:value", fromPackageName)
                }

                applicationNode.adoptChild("meta-data") {
                    setAttribute(
                        "android:name",
                        "$gmsCoreVendorGroupId.android.gms.SPOOFED_PACKAGE_SIGNATURE"
                    )
                    setAttribute("android:value", spoofedPackageSignature)
                }

                // GmsCore presence detection in extension.
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
        fun patchManifest() {
            val packageName = getPackageName(
                fromPackageName,
                packageNameYouTubeOption,
                packageNameYouTubeMusicOption
            )

            val transformations = mapOf(
                "package=\"$fromPackageName" to "package=\"$packageName",
                "android:authorities=\"$fromPackageName" to "android:authorities=\"$packageName",
                "$fromPackageName.permission.C2D_MESSAGE" to "$packageName.permission.C2D_MESSAGE",
                "$fromPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" to "$packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                "com.google.android.c2dm" to "$gmsCoreVendorGroupId.android.c2dm",
                "com.google.android.gms.permission.AD_ID" to "$gmsCoreVendorGroupId.android.gms.permission.AD_ID",
                "com.google.android.libraries.photos.api.mars" to "$gmsCoreVendorGroupId.android.apps.photos.api.mars",
                "com.google.android.providers.gsf.permission.READ_GSERVICES" to "$gmsCoreVendorGroupId.android.providers.gsf.permission.READ_GSERVICES",
                // TODO: Add this permission when bumping GmsCore
                // "com.google.android.gms.permission.ACTIVITY_RECOGNITION" to "$gmsCoreVendorGroupId.android.gms.permission.ACTIVITY_RECOGNITION",
            )

            // 'QUERY_ALL_PACKAGES' permission is required,
            // To check whether apps such as GmsCore, YouTube or YouTube Music are installed on the device.
            document("AndroidManifest.xml").use { document ->
                document.getElementsByTagName("manifest").item(0).also {
                    it.appendChild(
                        it.ownerDocument.createElement("uses-permission").also { element ->
                            element.setAttribute(
                                "android:name",
                                "android.permission.QUERY_ALL_PACKAGES"
                            )
                        })
                }
            }

            val manifest = get("AndroidManifest.xml")
            manifest.writeText(
                transformations.entries.fold(manifest.readText()) { acc, (from, to) ->
                    acc.replace(
                        from,
                        to,
                    )
                },
            )
        }

        patchManifest()
        addSpoofingMetadata()

        executeBlock()
    }

    block()
}
