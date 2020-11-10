package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz

expect fun DateTimeTz.formatDate(): String
expect fun DateTimeTz.formatTime(): String
expect fun TimeSoy.formatTime(): String