package com.krystianwsul.common.firebase

typealias DatabaseCallback = (String, Boolean, Exception?) -> Unit

fun Iterable<ChangeType>.reduce() = if (any { it == ChangeType.REMOTE }) ChangeType.REMOTE else ChangeType.LOCAL