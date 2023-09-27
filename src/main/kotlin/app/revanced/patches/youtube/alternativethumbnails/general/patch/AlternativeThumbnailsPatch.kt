package app.revanced.patches.youtube.alternativethumbnails.general.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.alternativethumbnails.general.fingerprints.CronetURLRequestCallbackOnResponseStartedFingerprint
import app.revanced.patches.youtube.alternativethumbnails.general.fingerprints.CronetURLRequestCallbackOnSucceededFingerprint
import app.revanced.patches.youtube.alternativethumbnails.general.fingerprints.MessageDigestImageUrlFingerprint
import app.revanced.patches.youtube.alternativethumbnails.general.fingerprints.MessageDigestImageUrlParentFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch.Companion.contexts
import app.revanced.util.resources.ResourceUtils.copyXmlNode

@Patch
@Name("Alternative thumbnails")
@Description("Adds an option to replace video thumbnails with still image captures of the video.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class AlternativeThumbnailsPatch : BytecodePatch(
    listOf(
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
                            invoke-static { p1 }, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideImageURL(Ljava/lang/String;)Ljava/lang/String;
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
                        "invoke-static { p2 }, $INTEGRATIONS_CLASS_DESCRIPTOR->handleCronetSuccess(Lorg/chromium/net/UrlResponseInfo;)V"
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

        SettingsPatch.updatePatchStatus("alternative-thumbnails")
    }

    internal companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "Lapp/revanced/integrations/alternativethumbnails/AlternativeThumbnailsPatch;"
    }
}
