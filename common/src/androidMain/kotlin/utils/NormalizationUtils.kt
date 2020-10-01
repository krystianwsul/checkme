package com.krystianwsul.common.utils

import java.text.Normalizer
import java.util.*

actual fun String.normalized(): String {
    if (!normalizedCache.containsKey(this)) {
        normalizedCache[this] = Normalizer.normalize(this, Normalizer.Form.NFKD)
                .replace(Regex("[\\p{M}]"), "")
                .toLowerCase(Locale.getDefault())
    }

    return normalizedCache.getValue(this)
}

private val normalizedCache = mutableMapOf<String, String>()