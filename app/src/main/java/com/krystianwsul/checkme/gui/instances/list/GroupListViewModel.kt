package com.krystianwsul.checkme.gui.instances.list

import androidx.lifecycle.ViewModel
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.utils.SingleCacheRelay
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class GroupListViewModel : ViewModel() {

    val compositeDisposable = CompositeDisposable()

    val copyInstanceKeyRelay = SingleCacheRelay<Data>()

    fun copy(instanceKey: InstanceKey) {
        DomainFactory.instanceRelay
            .filterNotNull()
            .firstOrError()
            .observeOnDomain()
            .map {
                Data(
                    instanceKey,
                    it.getInstance(instanceKey).taskHasOtherVisibleInstances(ExactTimeStamp.Local.now),
                )
            }
            .subscribe(copyInstanceKeyRelay)
            .addTo(compositeDisposable)
    }

    override fun onCleared() {
        compositeDisposable.clear()
    }

    data class Data(val instanceKey: InstanceKey, val showDialog: Boolean)
}