package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz

actual fun DateTimeTz.formatDate() = toString("yyyy-mm-dd")

actual fun DateTimeTz.formatTime() = toString("HH:mm")