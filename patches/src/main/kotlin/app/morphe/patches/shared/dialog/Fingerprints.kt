package app.morphe.patches.shared.dialog

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object CreateDialogFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "L", "Ljava/lang/String;"),
    filters = listOf(
        methodCall(smali = "Landroid/app/AlertDialog\$Builder;->setNegativeButton(ILandroid/content/DialogInterface\$OnClickListener;)Landroid/app/AlertDialog\$Builder;"),
        methodCall(smali = "Landroid/app/AlertDialog\$Builder;->setOnCancelListener(Landroid/content/DialogInterface\$OnCancelListener;)Landroid/app/AlertDialog\$Builder;"),
        methodCall(smali = "Landroid/app/AlertDialog\$Builder;->create()Landroid/app/AlertDialog;"),
        methodCall(smali = "Landroid/app/AlertDialog;->show()V")
    )
)

internal object CreateModernDialogFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.MOVE_RESULT),
        methodCall(smali = "Landroid/app/AlertDialog\$Builder;->setIcon(I)Landroid/app/AlertDialog\$Builder;"),
        methodCall(smali = "Landroid/app/AlertDialog\$Builder;->create()Landroid/app/AlertDialog;"),
    )
)

internal object PlayabilityStatusEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "OK",
        "ERROR",
        "UNPLAYABLE",
        "LOGIN_REQUIRED",
        "CONTENT_CHECK_REQUIRED",
        "AGE_CHECK_REQUIRED",
        "LIVE_STREAM_OFFLINE",
        "FULLSCREEN_ONLY",
        "GL_PLAYBACK_REQUIRED",
        "AGE_VERIFICATION_REQUIRED",
    )
)

internal object BackgroundPlaybackManagerShortsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L"),
    filters = listOf(
        literal(151635310)
    )
)

internal val ageVerifiedFingerprint = legacyFingerprint(
    name = "ageVerifiedFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Ljava/util/Map;"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    strings = listOf(
        "com.google.android.libraries.youtube.rendering.elements.sender_view",
        "com.google.android.libraries.youtube.innertube.endpoint.tag",
        "com.google.android.libraries.youtube.innertube.bundle",
        "com.google.android.libraries.youtube.logging.interaction_logger"
    )
)

internal val createDialogFingerprint = legacyFingerprint(
    name = "createDialogFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED.value,
    parameters = listOf("L", "L", "Ljava/lang/String;"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL // dialog.show()
    )
)

