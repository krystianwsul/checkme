package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.extensions.updateDeviceDbInfo
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.ChangeTypeSource
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class UserScope(
    factoryProvider: FactoryProvider,
    rootTasksFactory: RootTasksFactory,
    changeTypeSource: ChangeTypeSource,
    val myUserFactory: MyUserFactory,
    projectsFactorySingle: Single<ProjectsFactory>,
    friendsFactorySingle: Single<FriendsFactory>,
    notificationStorageSingle: Single<FactoryProvider.NotificationStorage>,
    shownFactorySingle: Single<Instance.ShownFactory>,
    tokenObservable: Observable<NullableWrapper<String>>,
    startTime: ExactTimeStamp.Local,
    domainDisposable: CompositeDisposable,
    getDeviceDbInfo: () -> DeviceDbInfo,
) {

    companion object {

        val instanceRelay = BehaviorRelay.createDefault(NullableWrapper<UserScope>())

        val nullableInstance get() = instanceRelay.value!!.value

        val instance get() = nullableInstance!!
    }

    val domainListenerManager = DomainListenerManager()

    init {
        Log.e("asdf", "magic userScope init") // todo scheduling
    }

    val domainFactorySingle = Single.zip(
        projectsFactorySingle.doOnSuccess { Log.e("asdf", "magic userScope projectsFactorySingle") }, // todo scheduling
        friendsFactorySingle.doOnSuccess { Log.e("asdf", "magic userScope friendsFactorySingle") }, // todo scheduling
        notificationStorageSingle.doOnSuccess {
            Log.e(
                "asdf",
                "magic userScope notificationStorageSingle"
            )
        }, // todo scheduling
        shownFactorySingle.doOnSuccess { Log.e("asdf", "magic userScope shownFactorySingle") }, // todo scheduling
    ) { projectsFactory, friendsFactory, notificationStorage, shownFactory ->
        factoryProvider.newDomain(
            shownFactory,
            myUserFactory,
            projectsFactory,
            friendsFactory,
            getDeviceDbInfo(),
            startTime,
            ExactTimeStamp.Local.now,
            domainDisposable,
            rootTasksFactory,
            notificationStorage,
            domainListenerManager,
        )
    }.cacheImmediate(domainDisposable)

    init {
        // ignore all change events that come in before the DomainFactory is initialized
        domainFactorySingle.flatMapObservable { domainFactory ->
            changeTypeSource.changeTypes.map { domainFactory to it }
        }
            .subscribe { (domainFactory, changeType) ->
                domainFactory.onChangeTypeEvent(changeType, ExactTimeStamp.Local.now)
            }
            .addTo(domainDisposable)

        tokenObservable.flatMapCompletable {
            factoryProvider.domainUpdater.updateDeviceDbInfo(getDeviceDbInfo())
        }
            .subscribe()
            .addTo(domainDisposable)
    }
}