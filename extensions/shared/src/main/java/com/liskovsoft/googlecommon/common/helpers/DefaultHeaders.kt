package com.liskovsoft.googlecommon.common.helpers

import android.os.Build

@Suppress("DEPRECATION", "unused")
object DefaultHeaders {
    private const val COBALT_VER = "23.lts.2.309559-gold"
    private const val V8_VER = "8.8.278.8-jit"
    private const val STARBOARD_VER = "14"
    private const val APK_VER = "3.02.006"

    // NOTE: Possible OOM exception!!! Uses more RAM because of decompression. Fast!
    private const val ACCEPT_ENCODING_COMPRESSED = "gzip, deflate, br" // NOTE: HttpUrlConnection won't work with compression

    // No compression. Optimal RAM usage! Slow.
    private const val ACCEPT_ENCODING_NONE = "identity"

    /// BEGIN TV Clients

    private const val USER_AGENT_SAMSUNG =
        "Mozilla/5.0 (Linux; Tizen 2.3; SmartHub; SMART-TV; SmartTV; U; Maple2012) AppleWebKit/538.1+ (KHTML, like Gecko) TV Safari/538.1+"
    private const val USER_AGENT_SAMSUNG_2 = "Mozilla/5.0 (SMART-TV; Linux; Tizen 2.4.0) AppleWebkit/538.1 (KHTML, like Gecko) SamsungBrowser/1.1 TV Safari/538.1"

    // Best (no throttling)
    private const val USER_AGENT_SAMSUNG_3 =
        "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15"

    // Bad. Doesn't contain 'Not recommend'/'Remove from history' context item
    private const val USER_AGENT_LG_2013 =
        "Mozilla/5.0 (Unknown; Linux armv7l) AppleWebKit/537.1+ (KHTML, like Gecko) Safari/537.1+ LG Browser/6.00.00(+mouse+3D+SCREEN+TUNER; LGE; 42LA660S-ZA; 04.25.05; 0x00000001;); LG NetCast.TV-2013 /04.25.05 (LG, 42LA660S-ZA, wired)"
    private const val USER_AGENT_COBALT_1 = "Mozilla/5.0 (DirectFB; Linux x86_64) Cobalt/4.13031-qa (unlike Gecko) Starboard/1"
    private const val USER_AGENT_COBALT_2 = "Mozilla/5.0 (DirectFB; Linux x86_64) Cobalt/20.lts.2.0-gold (unlike Gecko) Starboard/11"
    // See: https://github.com/youtube/cobalt/blob/main/cobalt/browser/user_agent/user_agent_platform_info.cc#L506
    private const val USER_AGENT_COBALT_3 = "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/25.lts.30.1034943-gold (unlike Gecko), Unknown_TV_Unknown_0/Unknown (Unknown, Unknown)"

    // OK
    private const val USER_AGENT_WEBOS =
        "Mozilla/5.0 (Web0S; Linux/SmartTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36 WebAppManager"
    private const val USER_AGENT_XBOX =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; Xbox; Xbox Series X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.82 Safari/537.36 Edge/20.02"

    private const val USER_AGENT_FIRE_TV =
        "Mozilla/5.0 (Linux armeabi-v7a; Android 7.1.2; Fire OS 6.0) Cobalt/22.lts.3.306369-gold (unlike Gecko) v8/8.8.278.8-jit gles Starboard/13, Amazon_ATV_mediatek8695_2019/NS6294 (Amazon, AFTMM, Wireless) com.amazon.firetv.youtube/22.3.r2.v66.0"
    private const val USER_AGENT_ATV =
        "Mozilla/5.0 (Linux armeabi-v7a; Android 7.1.2) Cobalt/23.lts.2.309559-gold (unlike Gecko) v8/8.8.278.8-jit gles Starboard/14, Amazon_ATV_mt8695_0/NS6294 (Amazon, AFTMM) com.google.android.youtube.tv/3.02.006"
    // Throttling! Probably, because of Chrome engine checks.
    private val USER_AGENT_ATV_COMBINED =
        "Mozilla/5.0 (Linux ${Build.CPU_ABI}; Android ${Build.VERSION.RELEASE}) Cobalt/$COBALT_VER (unlike Gecko) v8/$V8_VER gles Starboard/$STARBOARD_VER, ${Build.BRAND}_ATV_${Build.HARDWARE}_0/${Build.ID} (${Build.BRAND}, ${Build.MODEL}) com.google.android.youtube.tv/$APK_VER"

    /// END TV Clients

    // Throttling! Probably, because of Chrome engine checks.
    //private const val USER_AGENT_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36"
    private const val USER_AGENT_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    private const val USER_AGENT_MOBILE_CHROME_1 = "Mozilla/5.0 (Linux; Android 7.1.2; Redmi 4A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.86 Mobile Safari/537.36"
    private const val USER_AGENT_MOBILE_CHROME_2 = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
    private const val USER_AGENT_MOBILE_FIREFOX = "Mozilla/5.0 (iPhone; CPU iPhone OS 15_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML; like Gecko) FxiOS/98.2  Mobile/15E148 Safari/605.1.15"
    private const val USER_AGENT_PS_VITA = "Mozilla/5.0 (PlayStation Vita 3.74) AppleWebKit/537.73 (KHTML, like Gecko) Silk/3.2"
    private const val USER_AGENT_ANDROID_1 = "com.google.android.youtube/17.31.35 (Linux; U; Android 11) gzip"
    private const val USER_AGENT_IOS_1 = "com.google.ios.youtube/17.33.2 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)"
    private const val USER_AGENT_IOS_2 = "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)"
    const val USER_AGENT_SAFARI = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15,gzip(gfe)"

    const val USER_AGENT_TV = USER_AGENT_SAMSUNG_3 // no buffering
    //const val USER_AGENT_TV = USER_AGENT_ATV_COMBINED // buffering badly even with protobuf params (see videoinfo)
    //const val USER_AGENT_TV = USER_AGENT_FIRE_TV // buffering???
    const val USER_AGENT_WEB = USER_AGENT_CHROME
    const val USER_AGENT_MOBILE_WEB = USER_AGENT_MOBILE_FIREFOX
    const val USER_AGENT_ANDROID = USER_AGENT_ANDROID_1
    const val USER_AGENT_IOS = USER_AGENT_IOS_2

    @JvmField
    val APP_USER_AGENT = USER_AGENT_TV // no buffering

    const val ACCEPT_ENCODING = ACCEPT_ENCODING_COMPRESSED
    const val REFERER = "https://www.youtube.com/tv"
}
