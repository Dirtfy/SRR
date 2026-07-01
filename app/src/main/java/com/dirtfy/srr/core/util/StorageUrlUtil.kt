package com.dirtfy.srr.core.util

fun extractStoragePath(url: String): String? {
    return try {
        val oIdx = url.indexOf("/o/")
        if (oIdx < 0) return null
        val afterO = url.substring(oIdx + 3)
        val qIdx = afterO.indexOf('?')
        val encoded = if (qIdx >= 0) afterO.substring(0, qIdx) else afterO
        java.net.URLDecoder.decode(encoded, "UTF-8")
    } catch (_: Exception) { null }
}
