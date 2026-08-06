package app.morphe.patches.youtube.video.quality

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.newInstance
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.player.seekbar.VideoStreamingDataToStringFingerprint
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.cloneMutableAndPreserveParameters
import app.morphe.util.findFreeRegister
import app.morphe.util.insertLiteralOverride
import app.morphe.util.numberOfParameterRegistersLogical
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object VideoStreamingDataConstructorFingerprint : Fingerprint(
    classFingerprint = VideoStreamingDataToStringFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "Lcom/google/protos/youtube/api/innertube/StreamingDataOuterClass\$StreamingData;"
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        ),
        newInstance("Ljava/util/ArrayList;"),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "Lcom/google/protos/youtube/api/innertube/StreamingDataOuterClass\$StreamingData;"
        )
    ),
)

internal object PlatypusFeatureFlagPrimaryFingerprint : Fingerprint(
    filters = listOf(
        literal(45624008L)
    )
)

internal object PlatypusFeatureFlagSecondaryFingerprint : Fingerprint(
    filters = listOf(
        literal(45408049L)
    )
)

internal fun getPlaybackStartParametersConstructorFingerprint(
    initialResolutionField: FieldReference
) = object : Fingerprint(
    name = "<init>",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            reference = initialResolutionField
        )
    )
) {}

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/youtube/patches/playback/quality/PrioritizeVideoQualityPatch;"
private const val VIDEO_QUALITY_EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/youtube/patches/video/VideoQualityPatch;"

internal val prioritizeVideoQualityPatch = bytecodePatch {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    execute {
        // Fix initial default video quality.
        listOf(
            PlatypusFeatureFlagPrimaryFingerprint,
            PlatypusFeatureFlagSecondaryFingerprint
        ).forEach { fingerprint ->
            fingerprint.matchAll().forEach { match ->
                match.method.insertLiteralOverride(
                    match.instructionMatches.first().index,
                    "$VIDEO_QUALITY_EXTENSION_CLASS_DESCRIPTOR->overrideInitialVideoQualityFeatureFlag(Z)Z"
                )
            }
        }

        VideoStreamingDataConstructorFingerprint.match(
            VideoStreamingDataToStringFingerprint.classDef
        ).let { match ->
            // Clone method to preserve parameters.
            match.method.cloneMutableAndPreserveParameters(match.classDef).apply {
                // Must offset match indexes since cloning adds additional move instructions.
                val matchIndexOffset = match.method.numberOfParameterRegistersLogical
                val videoIdIndex = match.instructionMatches[1].index + matchIndexOffset
                val videoIdField = getInstruction<ReferenceInstruction>(videoIdIndex).reference

                val adaptiveFormatsIndex = match.instructionMatches.last().index + matchIndexOffset
                val adaptiveFormatsRegister = getInstruction<TwoRegisterInstruction>(adaptiveFormatsIndex).registerA

                val insertIndex = adaptiveFormatsIndex + 1
                val videoIdRegister = findFreeRegister(insertIndex, adaptiveFormatsRegister)

                addInstructions(
                    insertIndex,
                    """
                        # Get video id.
                        move-object/from16 v$videoIdRegister, p0
                        iget-object v$videoIdRegister, v$videoIdRegister, $videoIdField
                        
                        # Override adaptive formats.
                        invoke-static { v$videoIdRegister, v$adaptiveFormatsRegister }, $EXTENSION_CLASS_DESCRIPTOR->prioritizeVideoQuality(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
                        move-result-object v$adaptiveFormatsRegister
                    """
                )
            }
        }
    }
}
