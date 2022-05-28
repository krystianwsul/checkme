package com.krystianwsul.checkme.domainmodel

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.extensions.updateDeviceDbInfo
import com.krystianwsul.checkme.firebase.database.TaskPriorityMapperQueue
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.OwnedProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.ChangeTypeSource
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.mindorks.scheduler.Priority
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class UserScope(
    factoryProvider: FactoryProvider,
    rootTasksFactory: RootTasksFactory,
    changeTypeSource: ChangeTypeSource,
    val myUserFactory: MyUserFactory,
    projectsFactorySingle: Single<OwnedProjectsFactory>,
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

    val domainFactorySingle = Single.zip(
        projectsFactorySingle,
        friendsFactorySingle,
        notificationStorageSingle,
        shownFactorySingle,
        TaskPriorityMapperQueue.delayObservable
            .observeOnDomain(Priority.DB_NOTIFICATION_STORAGE)
            .switchMapSingle { it.getDelayCompletable(rootTasksFactory).toSingleDefault(Unit) }
            .firstOrError(),
    ) { projectsFactory, friendsFactory, notificationStorage, shownFactory, _ ->
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
        domainFactorySingle.flatMapObservable {
            changeTypeSource.remoteChanges.map { _ -> it }
        }
            .toFlowable(BackpressureStrategy.DROP) // this ensures that all changeTypes get "trampolined", and debounced to just one event
            .observeOn(getDomainScheduler(), false, 1)
            .subscribe { it.onRemoteChange(ExactTimeStamp.Local.now) }
            .addTo(domainDisposable)

        tokenObservable.flatMapCompletable { factoryProvider.domainUpdater.updateDeviceDbInfo(getDeviceDbInfo()) }
            .subscribe()
            .addTo(domainDisposable)
    }
}