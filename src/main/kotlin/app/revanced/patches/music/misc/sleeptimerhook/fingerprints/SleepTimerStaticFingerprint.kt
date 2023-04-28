package app.revanced.patches.music.misc.sleeptimerhook.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object SleepTimerStaticFingerprint : MethodFingerprint(
    returnType = "L",
    parameters = listOf("L"),
    strings = listOf("SLEEP_TIMER_MENU_BOTTOM_SHEET_FRAGMENT")
)
