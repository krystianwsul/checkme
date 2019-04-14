package com.krystianwsul.checkme.viewmodels

import java.io.Serializable

data class NullableWrapper<T>(val value: T? = null) : Serializable