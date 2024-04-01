package app.revanced.patches.shared.patch.microg

/**
 * constants for microG builds with signature spoofing
 */
object Constants {
    /**
     * microG vendor name
     * aka. package prefix / package base
     */
    const val MICROG_VENDOR = "app.revanced"

    /**
     * microG package name
     */
    const val MICROG_PACKAGE_NAME = "$MICROG_VENDOR.android.gms"

    /**
     * meta-data for microG package name spoofing on patched builds
     */
    const val META_SPOOFED_PACKAGE_NAME = "$MICROG_PACKAGE_NAME.SPOOFED_PACKAGE_NAME"

    /**
     * meta-data for microG package signature spoofing on patched builds
     */
    const val META_SPOOFED_PACKAGE_SIGNATURE =
        "$MICROG_PACKAGE_NAME.SPOOFED_PACKAGE_SIGNATURE"

    /**
     * meta-data for microG package detection
     */
    const val META_GMS_PACKAGE_NAME = "app.revanced.MICROG_PACKAGE_NAME"

    /**
     * a list of all permissions in microG
     */
    val PERMISSIONS = listOf(
        // C2DM / GCM
        "com.google.android.c2dm.permission.RECEIVE",
        "com.google.android.c2dm.permission.SEND",
        "com.google.android.gtalkservice.permission.GTALK_SERVICE",

        // GAuth
        "com.google.android.googleapps.permission.GOOGLE_AUTH",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.cp",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.local",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.mail",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.writely",
    )

    /**
     * a list of all (intent) actions in microG
     */
    val ACTIONS = listOf(
        // C2DM / GCM
        "com.google.android.c2dm.intent.REGISTER",
        "com.google.android.c2dm.intent.REGISTRATION",
        "com.google.android.c2dm.intent.UNREGISTER",
        "com.google.android.c2dm.intent.RECEIVE",
        "com.google.iid.TOKEN_REQUEST",
        "com.google.android.gcm.intent.SEND",

        // auth
        "com.google.android.gsf.login",
        "com.google.android.gsf.action.GET_GLS",
        "com.google.android.gms.common.account.CHOOSE_ACCOUNT",
        "com.google.android.gms.auth.login.LOGIN",
        "com.google.android.gms.auth.api.credentials.PICKER",
        "com.google.android.gms.auth.api.credentials.service.START",
        "com.google.android.gms.auth.service.START",
        "com.google.firebase.auth.api.gms.service.START",
        "com.google.android.gms.auth.be.appcert.AppCertService",

        // fido
        "com.google.android.gms.fido.fido2.privileged.START",

        // gass
        "com.google.android.gms.gass.START",

        // chimera
        "com.google.android.gms.chimera",

        // fonts
        "com.google.android.gms.fonts",

        // phenotype
        "com.google.android.gms.phenotype.service.START",

        // misc
        "com.google.android.gms.gmscompliance.service.START",
        "com.google.android.gms.oss.licenses.service.START",
        "com.google.android.gms.tapandpay.service.BIND",
        "com.google.android.gms.measurement.START",
        "com.google.android.gms.languageprofile.service.START",
        "com.google.android.gms.clearcut.service.START",
        "com.google.android.gms.icing.LIGHTWEIGHT_INDEX_SERVICE",

        // potoken
        "com.google.android.gms.potokens.service.START",

        // droidguard/ safetynet
        "com.google.android.gms.droidguard.service.START",
        "com.google.android.gms.safetynet.service.START"
    )

    /**
     * a list of all content provider authorities in microG
     */
    val AUTHORITIES = listOf(
        // gsf
        "com.google.android.gsf.gservices",
        "com.google.settings",

        // auth
        "com.google.android.gms.auth.accounts",

        // chimera
        "com.google.android.gms.chimera",

        // fonts
        "com.google.android.gms.fonts",

        // phenotype
        "com.google.android.gms.phenotype"
    )
}
