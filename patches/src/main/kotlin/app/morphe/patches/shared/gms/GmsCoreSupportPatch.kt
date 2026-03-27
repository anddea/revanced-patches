package app.morphe.patches.shared.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.Option
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchBuilder
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.extension.Constants.PATCHES_PATH
import app.morphe.patches.shared.gms.Constants.ACTIONS
import app.morphe.patches.shared.gms.Constants.ACTIONS_LEGACY
import app.morphe.patches.shared.gms.Constants.AUTHORITIES
import app.morphe.patches.shared.gms.Constants.AUTHORITIES_LEGACY
import app.morphe.patches.shared.gms.Constants.PERMISSIONS
import app.morphe.patches.shared.gms.Constants.PERMISSIONS_LEGACY
import app.morphe.util.Utils.printWarn
import app.morphe.util.Utils.trimIndentMultiline
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.methodOrNull
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.returnEarly
import app.morphe.util.valueOrThrow
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

private const val GMS_CORE_ORIGINAL_VENDOR_GROUP_ID = "com.google"
private const val GMS_CORE_REVANCED_VENDOR_GROUP_ID = "app.revanced"
private const val GMS_CORE_VANCED_VENDOR_GROUP_ID = "com.mgoogle"
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
    description = "Allows the app to work without root by using a different package name when patched " +
            "using a GmsCore instead of Google Play Services.",
) {
    val gmsCoreVendorGroupIdOption = stringOption(
        key = "gmsCoreVendorGroupId",
        default = GMS_CORE_REVANCED_VENDOR_GROUP_ID,
        values =
            mapOf(
                "ReVanced MicroG" to GMS_CORE_REVANCED_VENDOR_GROUP_ID,
                "Original MicroG" to GMS_CORE_ORIGINAL_VENDOR_GROUP_ID,
                "Vanced MicroG" to GMS_CORE_VANCED_VENDOR_GROUP_ID,
            ),
        title = "GmsCore vendor group ID",
        description = "The vendor's group ID for GmsCore. " +
                "Do not change this option, if you do not know what you are doing.",
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

    val disableCoreServices by booleanOption(
        key = "disableCoreServices",
        default = false,
        title = "Disable Core Services",
        description = """
            To reproduce the playback issue, several core services, including PoToken, are disabled.
            
            Do not enable this option unless you are testing at a low level.
            """.trimIndentMultiline(),
        required = false,
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
    ) { it!!.matches(Regex(PACKAGE_NAME_REGEX_PATTERN)) }

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
    ) { it!!.matches(Regex(PACKAGE_NAME_REGEX_PATTERN)) }

    val patchAllManifest by booleanOption(
        key = "patchAllManifest",
        default = false,
        title = "Patch all manifest components",
        description = "Patch all permissions, intents and content provider authorities supported by GmsCore.",
        required = true,
    )

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
        val patchAllManifestEnabled = patchAllManifest == true
        val permissions = if (patchAllManifestEnabled)
            PERMISSIONS
        else
            PERMISSIONS_LEGACY
        val actions = if (patchAllManifestEnabled)
            ACTIONS
        else
            ACTIONS_LEGACY
        val authorities = if (patchAllManifestEnabled)
            AUTHORITIES
        else
            AUTHORITIES_LEGACY

        fun transformStringReferences(transform: (str: String) -> String?) = classDefForEach {
            val mutableClass by lazy {
                mutableClassDefBy(it)
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
                in permissions,
                in actions,
                in authorities,
                    -> referencedString.replace("com.google", gmsCoreVendorGroupId!!)

                // TODO: Add this permission when bumping GmsCore
                // "android.media.MediaRouteProviderService" -> "$gmsCoreVendorGroupId.android.media.MediaRouteProviderService"
                else -> null
            }

        fun contentUrisTransform(str: String): String? {
            // only when content:// uri
            if (str.startsWith("content://")) {
                // check if matches any authority
                for (authority in authorities) {
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
                "$fromPackageName.SuggestionProvider",
                "$fromPackageName.fileprovider",
                    -> string.replace(fromPackageName, toPackageName)

                else -> null
            }
        }

        fun transformPrimeMethod(packageName: String) {
            if (patchAllManifestEnabled) {
                primeMethodFingerprint.methodOrNull()?.apply {
                    var register = 2

                    val index = instructions.indexOfFirst {
                        if (it.getReference<StringReference>()?.string != fromPackageName) return@indexOfFirst false

                        register = (it as OneRegisterInstruction).registerA
                        return@indexOfFirst true
                    }

                    replaceInstruction(index, "const-string v$register, \"$packageName\"")
                }
            } else {
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
        }

        // endregion

        val packageName = getPackageName(
            fromPackageName,
            gmsCoreVendorGroupIdOption,
            packageNameYouTubeOption,
            packageNameYouTubeMusicOption
        )

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

        val earlyReturnFingerprints = mutableListOf(
            castContextFetchFingerprint,
            googlePlayUtilityFingerprint,
            serviceCheckFingerprint,
        )

        if (disableCoreServices == true) {
            earlyReturnFingerprints += listOf(gmsServiceBrokerFingerprint)
        }

        if (patchAllManifestEnabled) {
            earlyReturnFingerprints += listOf(sslGuardFingerprint)

            // Prevent spam logs.
            eCatcherFingerprint.methodOrThrow().apply {
                val index = indexOfFirstInstructionOrThrow(Opcode.NEW_ARRAY)
                addInstruction(index, "return-void")
            }
        }

        // Return these methods early to prevent the app from crashing.
        earlyReturnFingerprints.forEach { it.methodOrThrow().returnEarly() }

        // Passes signature check of DroidGaurdResult (the.apk).
        droidGuardSignatureFingerprint
            .methodOrThrow().returnEarly(true)

        // Specific method that needs to be patched.
        transformPrimeMethod(packageName)

        // Verify GmsCore is installed and whitelisted for power optimizations and background usage.
        if (checkGmsCore == true) {
            mainActivityOnCreateFingerprint.method.apply {
                // Temporary fix for patches with an extension patch that hook the onCreate method as well.
                val setContextIndex = indexOfFirstInstruction {
                    val reference =
                        getReference<MethodReference>() ?: return@indexOfFirstInstruction false

                    reference.toString() == "Lapp/morphe/extension/shared/Utils;->setContext(Landroid/content/Context;)V"
                }

                // Add after setContext call, because this patch needs the context.
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

        mapOf(
            "PackageNameYouTube" to packageNameYouTubeOption.valueOrThrow(),
            "PackageNameYouTubeMusic" to packageNameYouTubeMusicOption.valueOrThrow()
        ).forEach { (methodName, value) ->
            findMethodOrThrow("$PATCHES_PATH/PatchStatus;") {
                name == methodName
            }.returnEarly(value)
        }

        executeBlock()
    }

    block()
}

/**
 * A collection of permissions, intents and content provider authorities
 * that are present in GmsCore which need to be transformed.
 */
private object Constants {
    /**
     * All permissions.
     */
    val PERMISSIONS = setOf(
        "com.google.android.c2dm.permission.RECEIVE",
        "com.google.android.c2dm.permission.SEND",
        "com.google.android.gms.auth.api.phone.permission.SEND",
        "com.google.android.gms.permission.AD_ID",
        "com.google.android.gms.permission.AD_ID_NOTIFICATION",
        "com.google.android.gms.permission.CAR_FUEL",
        "com.google.android.gms.permission.CAR_INFORMATION",
        "com.google.android.gms.permission.CAR_MILEAGE",
        "com.google.android.gms.permission.CAR_SPEED",
        "com.google.android.gms.permission.CAR_VENDOR_EXTENSION",
        "com.google.android.googleapps.permission.GOOGLE_AUTH",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.cp",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.local",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.mail",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.writely",
        "com.google.android.gtalkservice.permission.GTALK_SERVICE",
        "com.google.android.providers.gsf.permission.READ_GSERVICES",
    )

    val PERMISSIONS_LEGACY = setOf(
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

    /**
     * All intent actions.
     */
    val ACTIONS = setOf(
        "com.google.android.c2dm.intent.RECEIVE",
        "com.google.android.c2dm.intent.REGISTER",
        "com.google.android.c2dm.intent.REGISTRATION",
        "com.google.android.c2dm.intent.UNREGISTER",
        "com.google.android.contextmanager.service.ContextManagerService.START",
        "com.google.android.gcm.intent.SEND",
        "com.google.android.gms.accounts.ACCOUNT_SERVICE",
        "com.google.android.gms.accountsettings.ACCOUNT_PREFERENCES_SETTINGS",
        "com.google.android.gms.accountsettings.action.BROWSE_SETTINGS",
        "com.google.android.gms.accountsettings.action.VIEW_SETTINGS",
        "com.google.android.gms.accountsettings.MY_ACCOUNT",
        "com.google.android.gms.accountsettings.PRIVACY_SETTINGS",
        "com.google.android.gms.accountsettings.SECURITY_SETTINGS",
        "com.google.android.gms.ads.gservice.START",
        "com.google.android.gms.ads.identifier.service.EVENT_ATTESTATION",
        "com.google.android.gms.ads.service.CACHE",
        "com.google.android.gms.ads.service.CONSENT_LOOKUP",
        "com.google.android.gms.ads.service.HTTP",
        "com.google.android.gms.analytics.service.START",
        "com.google.android.gms.app.settings.GoogleSettingsLink",
        "com.google.android.gms.appstate.service.START",
        "com.google.android.gms.appusage.service.START",
        "com.google.android.gms.asterism.service.START",
        "com.google.android.gms.audiomodem.service.AudioModemService.START",
        "com.google.android.gms.audit.service.START",
        "com.google.android.gms.auth.account.authapi.START",
        "com.google.android.gms.auth.account.authenticator.auto.service.START",
        "com.google.android.gms.auth.account.authenticator.chromeos.START",
        "com.google.android.gms.auth.account.authenticator.tv.service.START",
        "com.google.android.gms.auth.account.data.service.START",
        "com.google.android.gms.auth.api.credentials.PICKER",
        "com.google.android.gms.auth.api.credentials.service.START",
        "com.google.android.gms.auth.api.identity.service.authorization.START",
        "com.google.android.gms.auth.api.identity.service.credentialsaving.START",
        "com.google.android.gms.auth.api.identity.service.signin.START",
        "com.google.android.gms.auth.api.phone.service.InternalService.START",
        "com.google.android.gms.auth.api.signin.service.START",
        "com.google.android.gms.auth.be.appcert.AppCertService",
        "com.google.android.gms.auth.blockstore.service.START",
        "com.google.android.gms.auth.config.service.START",
        "com.google.android.gms.auth.cryptauth.cryptauthservice.START",
        "com.google.android.gms.auth.GOOGLE_SIGN_IN",
        "com.google.android.gms.auth.login.LOGIN",
        "com.google.android.gms.auth.proximity.devicesyncservice.START",
        "com.google.android.gms.auth.proximity.securechannelservice.START",
        "com.google.android.gms.auth.proximity.START",
        "com.google.android.gms.auth.service.START",
        "com.google.android.gms.backup.ACTION_BACKUP_SETTINGS",
        "com.google.android.gms.backup.G1_BACKUP",
        "com.google.android.gms.backup.G1_RESTORE",
        "com.google.android.gms.backup.GMS_MODULE_RESTORE",
        "com.google.android.gms.beacon.internal.IBleService.START",
        "com.google.android.gms.car.service.START",
        "com.google.android.gms.carrierauth.service.START",
        "com.google.android.gms.cast.firstparty.START",
        "com.google.android.gms.cast.remote_display.service.START",
        "com.google.android.gms.cast.service.BIND_CAST_DEVICE_CONTROLLER_SERVICE",
        "com.google.android.gms.cast_mirroring.service.START",
        "com.google.android.gms.checkin.BIND_TO_SERVICE",
        "com.google.android.gms.chromesync.service.START",
        "com.google.android.gms.clearcut.service.START",
        "com.google.android.gms.common.account.CHOOSE_ACCOUNT",
        "com.google.android.gms.common.download.START",
        "com.google.android.gms.common.service.START",
        "com.google.android.gms.common.telemetry.service.START",
        "com.google.android.gms.config.START",
        "com.google.android.gms.constellation.service.START",
        "com.google.android.gms.credential.manager.service.firstparty.START",
        "com.google.android.gms.deviceconnection.service.START",
        "com.google.android.gms.drive.ApiService.RESET_AFTER_BOOT",
        "com.google.android.gms.drive.ApiService.START",
        "com.google.android.gms.drive.ApiService.STOP",
        "com.google.android.gms.droidguard.service.INIT",
        "com.google.android.gms.droidguard.service.PING",
        "com.google.android.gms.droidguard.service.START",
        "com.google.android.gms.enterprise.loader.service.START",
        "com.google.android.gms.facs.cache.service.START",
        "com.google.android.gms.facs.internal.service.START",
        "com.google.android.gms.feedback.internal.IFeedbackService",
        "com.google.android.gms.fido.credentialstore.internal_service.START",
        "com.google.android.gms.fido.fido2.privileged.START",
        "com.google.android.gms.fido.fido2.regular.START",
        "com.google.android.gms.fido.fido2.zeroparty.START",
        "com.google.android.gms.fido.sourcedevice.service.START",
        "com.google.android.gms.fido.targetdevice.internal_service.START",
        "com.google.android.gms.fido.u2f.privileged.START",
        "com.google.android.gms.fido.u2f.thirdparty.START",
        "com.google.android.gms.fido.u2f.zeroparty.START",
        "com.google.android.gms.fitness.BleApi",
        "com.google.android.gms.fitness.ConfigApi",
        "com.google.android.gms.fitness.GoalsApi",
        "com.google.android.gms.fitness.GoogleFitnessService.START",
        "com.google.android.gms.fitness.HistoryApi",
        "com.google.android.gms.fitness.InternalApi",
        "com.google.android.gms.fitness.RecordingApi",
        "com.google.android.gms.fitness.SensorsApi",
        "com.google.android.gms.fitness.SessionsApi",
        "com.google.android.gms.fonts.service.START",
        "com.google.android.gms.freighter.service.START",
        "com.google.android.gms.games.internal.connect.service.START",
        "com.google.android.gms.games.PLAY_GAMES_UPGRADE",
        "com.google.android.gms.games.service.START",
        "com.google.android.gms.gass.START",
        "com.google.android.gms.gmscompliance.service.START",
        "com.google.android.gms.googlehelp.HELP",
        "com.google.android.gms.googlehelp.service.GoogleHelpService.START",
        "com.google.android.gms.growth.service.START",
        "com.google.android.gms.herrevad.services.LightweightNetworkQualityAndroidService.START",
        "com.google.android.gms.icing.INDEX_SERVICE",
        "com.google.android.gms.icing.LIGHTWEIGHT_INDEX_SERVICE",
        "com.google.android.gms.identity.service.BIND",
        "com.google.android.gms.inappreach.service.START",
        "com.google.android.gms.instantapps.START",
        "com.google.android.gms.kids.service.START",
        "com.google.android.gms.languageprofile.service.START",
        "com.google.android.gms.learning.internal.dynamitesupport.START",
        "com.google.android.gms.learning.intservice.START",
        "com.google.android.gms.learning.predictor.START",
        "com.google.android.gms.learning.trainer.START",
        "com.google.android.gms.learning.training.background.START",
        "com.google.android.gms.location.places.GeoDataApi",
        "com.google.android.gms.location.places.PlaceDetectionApi",
        "com.google.android.gms.location.places.PlacesApi",
        "com.google.android.gms.location.reporting.service.START",
        "com.google.android.gms.location.settings.LOCATION_HISTORY",
        "com.google.android.gms.location.settings.LOCATION_REPORTING_SETTINGS",
        "com.google.android.gms.locationsharing.api.START",
        "com.google.android.gms.locationsharingreporter.service.START",
        "com.google.android.gms.lockbox.service.START",
        "com.google.android.gms.matchstick.lighter.service.START",
        "com.google.android.gms.mdm.services.DeviceManagerApiService.START",
        "com.google.android.gms.mdm.services.START",
        "com.google.android.gms.mdns.service.START",
        "com.google.android.gms.measurement.START",
        "com.google.android.gms.nearby.bootstrap.service.NearbyBootstrapService.START",
        "com.google.android.gms.nearby.connection.service.START",
        "com.google.android.gms.nearby.fastpair.START",
        "com.google.android.gms.nearby.messages.service.NearbyMessagesService.START",
        "com.google.android.gms.nearby.sharing.service.NearbySharingService.START",
        "com.google.android.gms.nearby.sharing.START_SERVICE",
        "com.google.android.gms.notifications.service.START",
        "com.google.android.gms.ocr.service.internal.START",
        "com.google.android.gms.ocr.service.START",
        "com.google.android.gms.oss.licenses.service.START",
        "com.google.android.gms.payse.service.BIND",
        "com.google.android.gms.people.contactssync.service.START",
        "com.google.android.gms.people.service.START",
        "com.google.android.gms.phenotype.service.START",
        "com.google.android.gms.photos.autobackup.service.START",
        "com.google.android.gms.playlog.service.START",
        "com.google.android.gms.plus.service.default.INTENT",
        "com.google.android.gms.plus.service.image.INTENT",
        "com.google.android.gms.plus.service.internal.START",
        "com.google.android.gms.plus.service.START",
        "com.google.android.gms.potokens.service.START",
        "com.google.android.gms.pseudonymous.service.START",
        "com.google.android.gms.rcs.START",
        "com.google.android.gms.reminders.service.START",
        "com.google.android.gms.romanesco.MODULE_BACKUP_AGENT",
        "com.google.android.gms.romanesco.service.START",
        "com.google.android.gms.safetynet.service.START",
        "com.google.android.gms.scheduler.ACTION_PROXY_SCHEDULE",
        "com.google.android.gms.search.service.SEARCH_AUTH_START",
        "com.google.android.gms.semanticlocation.service.START_ODLH",
        "com.google.android.gms.sesame.service.BIND",
        "com.google.android.gms.settings.EXPOSURE_NOTIFICATION_SETTINGS",
        "com.google.android.gms.setup.auth.SecondDeviceAuth.START",
        "com.google.android.gms.signin.service.START",
        "com.google.android.gms.smartdevice.d2d.SourceDeviceService.START",
        "com.google.android.gms.smartdevice.d2d.TargetDeviceService.START",
        "com.google.android.gms.smartdevice.directtransfer.SourceDirectTransferService.START",
        "com.google.android.gms.smartdevice.directtransfer.TargetDirectTransferService.START",
        "com.google.android.gms.smartdevice.postsetup.PostSetupService.START",
        "com.google.android.gms.smartdevice.setup.accounts.AccountsService.START",
        "com.google.android.gms.smartdevice.wifi.START_WIFI_HELPER_SERVICE",
        "com.google.android.gms.social.location.activity.service.START",
        "com.google.android.gms.speech.service.START",
        "com.google.android.gms.statementservice.EXECUTE",
        "com.google.android.gms.stats.ACTION_UPLOAD_DROPBOX_ENTRIES",
        "com.google.android.gms.tapandpay.service.BIND",
        "com.google.android.gms.telephonyspam.service.START",
        "com.google.android.gms.testsupport.service.START",
        "com.google.android.gms.thunderbird.service.START",
        "com.google.android.gms.trustagent.BridgeApi.START",
        "com.google.android.gms.trustagent.StateApi.START",
        "com.google.android.gms.trustagent.trustlet.trustletmanagerservice.BIND",
        "com.google.android.gms.trustlet.bluetooth.service.BIND",
        "com.google.android.gms.trustlet.connectionlessble.service.BIND",
        "com.google.android.gms.trustlet.face.service.BIND",
        "com.google.android.gms.trustlet.nfc.service.BIND",
        "com.google.android.gms.trustlet.onbody.service.BIND",
        "com.google.android.gms.trustlet.place.service.BIND",
        "com.google.android.gms.trustlet.voiceunlock.service.BIND",
        "com.google.android.gms.udc.service.START",
        "com.google.android.gms.update.START_API_SERVICE",
        "com.google.android.gms.update.START_SERVICE",
        "com.google.android.gms.update.START_SINGLE_USER_API_SERVICE",
        "com.google.android.gms.update.START_TV_API_SERVICE",
        "com.google.android.gms.usagereporting.service.START",
        "com.google.android.gms.userlocation.service.START",
        "com.google.android.gms.vehicle.cabin.service.START",
        "com.google.android.gms.vehicle.climate.service.START",
        "com.google.android.gms.vehicle.info.service.START",
        "com.google.android.gms.wallet.service.BIND",
        "com.google.android.gms.walletp2p.service.firstparty.BIND",
        "com.google.android.gms.walletp2p.service.zeroparty.BIND",
        "com.google.android.gms.wearable.BIND",
        "com.google.android.gms.wearable.BIND_LISTENER",
        "com.google.android.gms.wearable.DATA_CHANGED",
        "com.google.android.gms.wearable.MESSAGE_RECEIVED",
        "com.google.android.gms.wearable.NODE_CHANGED",
        "com.google.android.gsf.action.GET_GLS",
        "com.google.android.location.settings.LOCATION_REPORTING_SETTINGS",
        "com.google.android.mdd.service.START",
        "com.google.android.mdh.service.listener.START",
        "com.google.android.mdh.service.START",
        "com.google.android.mobstore.service.START",
        "com.google.firebase.auth.api.gms.service.START",
        "com.google.firebase.dynamiclinks.service.START",
        "com.google.iid.TOKEN_REQUEST",
        "com.google.android.gms.location.places.ui.PICK_PLACE",
    )

    val ACTIONS_LEGACY = setOf(
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

    /**
     * All content provider authorities.
     */
    val AUTHORITIES = setOf(
        "com.google.android.gms.auth.accounts",
        "com.google.android.gms.chimera",
        "com.google.android.gms.fonts",
        "com.google.android.gms.phenotype",
        "com.google.android.gsf.gservices",
        "com.google.settings",
    )

    val AUTHORITIES_LEGACY = setOf(
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

private fun printPackageNameWarn(packageName: String) =
    printWarn("Invalid package name was used: $packageName. ROMs with pre-installed GApps cannot install patched apps.")

private fun getPackageName(
    originalPackageName: String,
    gmsCoreVendorGroupIdOption: Option<String>,
    packageNameYouTubeOption: Option<String>,
    packageNameYouTubeMusicOption: Option<String>,
): String {
    val gmsCoreVendorGroupId = gmsCoreVendorGroupIdOption.valueOrThrow()
    if (originalPackageName == ORIGINAL_PACKAGE_NAME_YOUTUBE) {
        val packageName = packageNameYouTubeOption.valueOrThrow()
        if (packageName == ORIGINAL_PACKAGE_NAME_YOUTUBE &&
            gmsCoreVendorGroupId != GMS_CORE_ORIGINAL_VENDOR_GROUP_ID
        ) {
            printPackageNameWarn(packageName)
        }
        return packageName
    } else if (originalPackageName == ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC) {
        val packageName = packageNameYouTubeMusicOption.valueOrThrow()
        if (packageName == ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC &&
            gmsCoreVendorGroupId != GMS_CORE_ORIGINAL_VENDOR_GROUP_ID
        ) {
            printPackageNameWarn(packageName)
        }
        return packageName
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
                gmsCoreVendorGroupIdOption,
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
