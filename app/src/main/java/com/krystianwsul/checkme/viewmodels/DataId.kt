package com.krystianwsul.checkme.viewmodels

import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import kotlinx.parcelize.Parcelize

@Parcelize
data class DataId(val value: Int) : Parcelable {

    fun toFirst() = DomainListenerManager.NotificationType.First(this)
    fun toSkip() = DomainListenerManager.NotificationType.Skip(this)
}