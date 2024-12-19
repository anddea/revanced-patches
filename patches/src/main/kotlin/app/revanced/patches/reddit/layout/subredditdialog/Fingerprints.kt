package app.revanced.patches.reddit.layout.subredditdialog

import app.revanced.patches.reddit.utils.resourceid.cancelButton
import app.revanced.patches.reddit.utils.resourceid.textAppearanceRedditBaseOldButtonColored
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val frequentUpdatesSheetScreenFingerprint = legacyFingerprint(
    name = "frequentUpdatesSheetScreenFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(cancelButton),
    customFingerprint = { _, classDef ->
        classDef.sourceFile == "FrequentUpdatesSheetScreen.kt"
    }
)

internal val redditAlertDialogsFingerprint = legacyFingerprint(
    name = "redditAlertDialogsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(textAppearanceRedditBaseOldButtonColored),
    customFingerprint = { _, classDef ->
        classDef.sourceFile == "RedditAlertDialogs.kt"
    }
)