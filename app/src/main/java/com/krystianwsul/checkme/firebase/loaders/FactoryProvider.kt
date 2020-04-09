package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.ProjectFactory
import com.krystianwsul.checkme.firebase.RemoteUserFactory
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.Observable

interface FactoryProvider {

    val nullableInstance: Domain?

    val database: Database

    val preferences: Preferences

    val projectProvider: ProjectProvider

    fun newDomain(
            localFactory: Local,
            remoteUserFactory: RemoteUserFactory,
            projectFactory: ProjectFactory,
            deviceDbInfo: DeviceDbInfo,
            startTime: ExactTimeStamp,
            readTime: ExactTimeStamp,
            friendSnapshot: Database.Snapshot
    ): Domain

    interface Domain {

        fun updatePrivateProjectRecord(dataSnapshot: Database.Snapshot)

        fun updateSharedProjectRecords(sharedProjectEvent: ProjectFactory.SharedProjectEvent)

        fun updateFriendRecords(dataSnapshot: Database.Snapshot)

        fun updateUserRecord(dataSnapshot: Database.Snapshot)

        fun updateInstanceRecords(instanceEvent: ProjectFactory.InstanceEvent)

        fun clearUserInfo()
    }

    interface Local : Instance.ShownFactory {

        val uuid: String
    }

    abstract class Database : ProjectProvider.Database() {

        abstract fun getUserObservable(key: UserKey): Observable<Snapshot>

        abstract fun getPrivateProjectObservable(key: ProjectKey.Private): Observable<Snapshot>

        abstract fun getFriendObservable(userKey: UserKey): Observable<Snapshot>

        abstract fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot>

        interface Snapshot {

            val key: String

            val children: Iterable<Snapshot>

            fun exists(): Boolean

            fun <T> getValue(valueType: Class<T>): T?

            fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>): T?

            class Impl(private val dataSnapshot: DataSnapshot) : Snapshot {

                override val key get() = dataSnapshot.key!!

                override val children get() = dataSnapshot.children.map { Impl(it) }

                override fun exists() = dataSnapshot.exists()

                override fun <T> getValue(valueType: Class<T>) = dataSnapshot.getValue(valueType)

                override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>) = dataSnapshot.getValue(genericTypeIndicator)
            }
        }
    }

    interface Preferences {

        var tab: Int
    }

    object Impl : FactoryProvider {

        override val nullableInstance get() = DomainFactory.nullableInstance

        override val database = AndroidDatabaseWrapper

        override val preferences = com.krystianwsul.checkme.Preferences

        override val projectProvider = object : ProjectProvider {

            override val database = this@Impl.database
        }

        override fun newDomain(
                localFactory: Local,
                remoteUserFactory: RemoteUserFactory,
                projectFactory: ProjectFactory,
                deviceDbInfo: DeviceDbInfo,
                startTime: ExactTimeStamp,
                readTime: ExactTimeStamp,
                friendSnapshot: Database.Snapshot
        ) = DomainFactory(
                localFactory as LocalFactory,
                remoteUserFactory,
                projectFactory,
                deviceDbInfo,
                startTime,
                readTime,
                friendSnapshot
        )
    }
}