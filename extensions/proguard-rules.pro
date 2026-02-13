## Combined all the proguard-rules.pro into this one file, because I kept running into issues with
## the separate ones.
-dontobfuscate
-dontoptimize
-keepattributes *
-keep class app.morphe.** {
  *;
}
-keep class com.google.** {
  *;
}
-keep class com.aurora.** {
  *;
}
-keep class com.dragons.** {
  *;
}
-keep class com.eclipsesource.v8.** {
  *;
}
-keep class com.liskovsoft.** {
  *;
}

## Rules for OkHttp. Copy pasted from https://github.com/square/okhttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.lang.model.element.Modifier
