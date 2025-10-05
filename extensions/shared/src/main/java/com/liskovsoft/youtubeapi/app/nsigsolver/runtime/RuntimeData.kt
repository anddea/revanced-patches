package com.liskovsoft.youtubeapi.app.nsigsolver.runtime

import java.security.MessageDigest

internal enum class ScriptType(val value: String) {
    LIB("lib"),
    CORE("core");
}

internal enum class ScriptVariant(val value: String) {
    UNKNOWN("unknown"),
    MINIFIED("minified"),
    UNMINIFIED("unminified"),
    DENO_NPM("deno_npm"),
    BUN_NPM("bun_npm"),
    V8_NPM("v8_npm");
}

internal enum class ScriptSource(val value: String) {
    PYPACKAGE("python package"),
    BINARY("binary"),
    CACHE("cache"),
    WEB("web"),
    BUILTIN("builtin");
}

internal data class Script(
    val type: ScriptType,
    val variant: ScriptVariant,
    val source: ScriptSource,
    val version: String,
    val code: String,
) {
    val hash: String by lazy {
        val digest = MessageDigest.getInstance("SHA3-512")
        val bytes = digest.digest(code.toByteArray(Charsets.UTF_8))
        bytes.joinToString("") { "%02x".format(it) }
    }

    override fun toString(): String {
        return "<Script ${type.value} v${version} (source: ${source.value}) variant=${variant.value} size=${code.length} hash=${
            hash.take(
                7
            )
        }...>"
    }
}
