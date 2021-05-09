package com.krystianwsul.common.utils

import kotlin.jvm.JvmInline

@JvmInline
value class TaskHierarchyId(val value: String) {

    override fun toString() = value // todo backend not sure if this is needed
}