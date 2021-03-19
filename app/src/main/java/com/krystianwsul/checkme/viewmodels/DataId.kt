package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainListenerManager

data class DataId(val value: Int) {

    fun toFirst() = DomainListenerManager.NotificationType.First(this)
    fun toSkip() = DomainListenerManager.NotificationType.Skip(this)
}