package com.krystianwsul.common.utils

import java.text.Normalizer

actual fun String.normalized(): String {
    if (!normalizedCache.containsKey(this)) {
        normalizedCache[this] = Normalizer.normalize(this, Normalizer.Form.NFKD)
            .replace(Regex("[\\p{M}]"), "")
            .replace('ł', 'l')
            .lowercase()
    }

    return normalizedCache[this] ?: throw StrangeNormalizationException(this)
}

private class StrangeNormalizationException(str: String) : Exception("strange normalization for $str")

private val normalizedCache = mutableMapOf<String, String>()