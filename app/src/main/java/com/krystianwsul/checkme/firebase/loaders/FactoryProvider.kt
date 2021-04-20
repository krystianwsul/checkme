package com.krystianwsul.checkme.firebase.loaders

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

interface FactoryProvider {

    val nullableInstance: Domain?

    val database: Database

    val projectProvider: ProjectProvider

    val shownFactory: Instance.ShownFactory

    val sharedProjectsProvider
        get() = object : SharedProjectsProvider {

            override val projectProvider = this@FactoryProvider.projectProvider

            override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) =
                    database.getSharedProjectObservable(projectKey)
        }

    val friendsProvider
        get() = object : FriendsProvider {

            override val database = this@FactoryProvider.database
        }

    val domainUpdater: DomainUpdater

    fun newDomain(
            localFactory: Local,
            myUserFactory: MyUserFactory,
            projectsFactory: ProjectsFactory,
            friendsFactory: FriendsFactory,
            deviceDbInfo: DeviceDbInfo,
            startTime: ExactTimeStamp.Local,
            readTime: ExactTimeStamp.Local,
            domainDisposable: CompositeDisposable,
    ): Domain

    interface Domain {

        fun onChangeTypeEvent(changeType: ChangeType, now: ExactTimeStamp.Local)

        @CheckResult
        fun clearUserInfo(): Completable
    }

    interface Local : Instance.ShownFactory {

        val uuid: String
    }

    abstract class Database : FriendsProvider.Database() {

        abstract fun getPrivateProjectObservable(key: ProjectKey.Private): Observable<Snapshot<PrivateProjectJson>>

        abstract fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot<JsonWrapper>>
    }

    class Impl(override val shownFactory: Instance.ShownFactory) : FactoryProvider {

        override val nullableInstance get() = DomainFactory.nullableInstance

        override val database = AndroidDatabaseWrapper

        override val projectProvider = object : ProjectProvider {

            override val database = this@Impl.database
        }

        override val domainUpdater = AndroidDomainUpdater

        override fun newDomain(
                localFactory: Local,
                myUserFactory: MyUserFactory,
                projectsFactory: ProjectsFactory,
                friendsFactory: FriendsFactory,
                deviceDbInfo: DeviceDbInfo,
                startTime: ExactTimeStamp.Local,
                readTime: ExactTimeStamp.Local,
                domainDisposable: CompositeDisposable,
        ) = DomainFactory(
                localFactory as LocalFactory,
                myUserFactory,
                projectsFactory,
                friendsFactory,
                deviceDbInfo,
                startTime,
                readTime,
                domainDisposable,
                database,
        ) { AndroidDomainUpdater }
    }
}