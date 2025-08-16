package app.revanced.patches.youtube.player.flyoutmenu.hide

import app.revanced.patches.youtube.utils.indexOfAddHeaderViewInstruction
import app.revanced.patches.youtube.utils.resourceid.bottomSheetFooterText
import app.revanced.patches.youtube.utils.resourceid.subtitleMenuSettingsFooterInfo
import app.revanced.patches.youtube.utils.resourceid.videoQualityBottomSheet
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val advancedQualityBottomSheetFingerprint = legacyFingerprint(
    name = "advancedQualityBottomSheetFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L", "L"),
    customFingerprint = custom@{ method, _ ->
        if (!method.containsLiteralInstruction(videoQualityBottomSheet)) {
            return@custom false
        }
        if (indexOfAddHeaderViewInstruction(method) < 0) {
            return@custom false
        }
        val implementation = method.implementation
            ?: return@custom false

        implementation.instructions.elementAt(0).opcode == Opcode.IGET_OBJECT
    }
)

internal val captionsBottomSheetFingerprint = legacyFingerprint(
    name = "captionsBottomSheetFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(bottomSheetFooterText, subtitleMenuSettingsFooterInfo),
)

/**
 * This fingerprint is compatible with YouTube v18.39.xx+
 */
internal val pipModeConfigFingerprint = legacyFingerprint(
    name = "pipModeConfigFingerprint",
    literals = listOf(45427407L),
)

internal const val SLEEP_TIMER_CONSTRUCTOR_FEATURE_FLAG = 45640654L

internal val sleepTimerConstructorFingerprint = legacyFingerprint(
    name = "sleepTimerConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(SLEEP_TIMER_CONSTRUCTOR_FEATURE_FLAG),
)

internal const val SLEEP_TIMER_FEATURE_FLAG = 45630421L

internal val sleepTimerFingerprint = legacyFingerprint(
    name = "sleepTimerConstructorFingerprint",
    returnType = "Z",
    literals = listOf(SLEEP_TIMER_FEATURE_FLAG),
)
