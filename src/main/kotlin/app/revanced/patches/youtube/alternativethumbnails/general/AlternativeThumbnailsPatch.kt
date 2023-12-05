package app.revanced.patches.youtube.alternativethumbnails.general

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.alternativethumbnails.general.fingerprints.CronetURLRequestCallbackOnResponseStartedFingerprint
import app.revanced.patches.youtube.alternativethumbnails.general.fingerprints.CronetURLRequestCallbackOnSucceededFingerprint
import app.revanced.patches.youtube.alternativethumbnails.general.fingerprints.MessageDigestImageUrlFingerprint
import app.revanced.patches.youtube.alternativethumbnails.general.fingerprints.MessageDigestImageUrlParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.ALTERNATIVE_THUMBNAILS
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch.contexts
import app.revanced.util.copyXmlNode
import app.revanced.util.exception

@Patch(
    name = "Alternative thumbnails",
    description = "Adds an option to replace video thumbnails with still image captures of the video.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43"
            ]
        )
    ]
)
@Suppress("unused")
object AlternativeThumbnailsPatch : BytecodePatch(
    setOf(
        CronetURLRequestCallbackOnResponseStartedFingerprint,
        MessageDigestImageUrlParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Hook should should come first.
         */
        MessageDigestImageUrlParentFingerprint.result?.let { parentResult ->
            MessageDigestImageUrlFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    addInstructions(
                        0, """
                            invoke-static { p1 }, $ALTERNATIVE_THUMBNAILS->overrideImageURL(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object p1
                            """
                    )
                }
            } ?: throw MessageDigestImageUrlFingerprint.exception
        } ?: throw MessageDigestImageUrlParentFingerprint.exception


        /**
         * If a connection completed, which includes normal 200 responses but also includes
         * status 404 and other error like http responses.
         */
        CronetURLRequestCallbackOnResponseStartedFingerprint.result?.let { parentResult ->
            CronetURLRequestCallbackOnSucceededFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    addInstruction(
                        0,
                        "invoke-static { p2 }, $ALTERNATIVE_THUMBNAILS->handleCronetSuccess(Lorg/chromium/net/UrlResponseInfo;)V"
                    )
                }
            } ?: throw CronetURLRequestCallbackOnSucceededFingerprint.exception
        } ?: throw CronetURLRequestCallbackOnResponseStartedFingerprint.exception

        /**
         * Copy arrays
         */
        contexts.copyXmlNode("youtube/alternativethumbnails/host", "values/arrays.xml", "resources")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: ALTERNATIVE_THUMBNAILS_SETTINGS"
            )
        )

        SettingsPatch.updatePatchStatus("Alternative thumbnails")
    }
}
