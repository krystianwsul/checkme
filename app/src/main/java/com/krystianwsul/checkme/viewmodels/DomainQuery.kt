package com.krystianwsul.checkme.viewmodels

interface DomainQuery<DOMAIN_DATA : DomainData> {

    fun getDomainResult(): DomainResult<DOMAIN_DATA>

    fun interrupt() = Unit
}