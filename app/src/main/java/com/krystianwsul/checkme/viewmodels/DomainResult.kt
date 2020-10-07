package com.krystianwsul.checkme.viewmodels

sealed class DomainResult<T : DomainData> {

    open val data: T? = null

    class Interrupted<T : DomainData> : DomainResult<T>()

    data class Completed<T : DomainData>(override val data: T) : DomainResult<T>()
}