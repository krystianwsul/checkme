package com.krystianwsul.checkme.firebase.loaders

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.notifications.AndroidShownFactory
import com.krystianwsul.checkme.domainmodel.notifications.InstanceShownData
import com.krystianwsul.checkme.domainmodel.notifications.ProjectNotificationKey
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.OwnedProjectsFactory
import com.krystianwsul.checkme.firebase.foreignProjects.ForeignProjectsFactory
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.firebase.roottask.RootTasksLoader
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateForeignProjectJson
import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.firebase.json.projects.SharedForeignProjectJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

interface FactoryProvider {

    val nullableInstance: Domain?

    val database: Database

    val projectProvider: ProjectProvider

    val sharedProjectsProvider
        get() = object : SharedProjectsProvider {

            override val projectProvider = this@FactoryProvider.projectProvider

            override fun getProjectObservable(projectKey: ProjectKey<out ProjectType.Shared>) =
                database.getSharedOwnedProjectObservable(projectKey as ProjectKey.Shared)
        }

    val friendsProvider
        get() = object : FriendsProvider {

            override val database = this@FactoryProvider.database
        }

    val domainUpdater: DomainUpdater

    val notificationStorageFactory: NotificationStorageFactory

    val uuid: String

    fun newShownFactory(notificationStorage: NotificationStorage): Instance.ShownFactory

    fun newDomain(
        shownFactory: Instance.ShownFactory,
        myUserFactory: MyUserFactory,
        projectsFactory: OwnedProjectsFactory,
        friendsFactory: FriendsFactory,
        deviceDbInfo: DeviceDbInfo,
        startTime: ExactTimeStamp.Local,
        readTime: ExactTimeStamp.Local,
        domainDisposable: CompositeDisposable,
        rootTasksFactory: RootTasksFactory,
        notificationStorage: NotificationStorage,
        domainListenerManager: DomainListenerManager,
        foreignProjectsFactory: ForeignProjectsFactory,
    ): Domain

    interface Domain {

        fun onRemoteChange(now: ExactTimeStamp.Local)

        @CheckResult
        fun clearUserInfo(): Completable
    }

    interface NotificationStorageFactory {

        fun getNotificationStorage(): Single<NotificationStorage>
    }

    interface NotificationStorage {

        var projectNotificationKeys: List<ProjectNotificationKey>
        val instanceShownMap: MutableMap<InstanceKey, InstanceShownData>

        fun save(): Boolean

        fun deleteInstanceShown(taskKeys: Set<TaskKey>)
    }

    abstract class Database : FriendsProvider.Database(), RootTasksLoader.Provider {

        abstract fun getPrivateOwnedProjectObservable(projectKey: ProjectKey.Private): Observable<Snapshot<PrivateOwnedProjectJson>>
        abstract fun getSharedOwnedProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot<JsonWrapper>>

        abstract fun getPrivateForeignProjectObservable(projectKey: ProjectKey.Private): Observable<Snapshot<PrivateForeignProjectJson>>
        abstract fun getSharedForeignProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot<SharedForeignProjectJson>>
    }

    class Impl(override val uuid: String) : FactoryProvider {

        override val nullableInstance get() = DomainFactory.nullableInstance

        override val database = AndroidDatabaseWrapper

        override val projectProvider = object : ProjectProvider {

            override val database = this@Impl.database
        }

        override val domainUpdater = AndroidDomainUpdater

        override val notificationStorageFactory =
            com.krystianwsul.checkme.domainmodel.notifications.NotificationStorage.Companion

        override fun newShownFactory(notificationStorage: NotificationStorage) = AndroidShownFactory(notificationStorage)

        override fun newDomain(
            shownFactory: Instance.ShownFactory,
            myUserFactory: MyUserFactory,
            projectsFactory: OwnedProjectsFactory,
            friendsFactory: FriendsFactory,
            deviceDbInfo: DeviceDbInfo,
            startTime: ExactTimeStamp.Local,
            readTime: ExactTimeStamp.Local,
            domainDisposable: CompositeDisposable,
            rootTasksFactory: RootTasksFactory,
            notificationStorage: NotificationStorage,
            domainListenerManager: DomainListenerManager,
            foreignProjectsFactory: ForeignProjectsFactory,
        ) = DomainFactory(
            shownFactory,
            myUserFactory,
            projectsFactory,
            friendsFactory,
            deviceDbInfo,
            startTime,
            readTime,
            domainDisposable,
            database,
            rootTasksFactory,
            notificationStorage,
            domainListenerManager,
            foreignProjectsFactory,
        ) { AndroidDomainUpdater }
    }
}