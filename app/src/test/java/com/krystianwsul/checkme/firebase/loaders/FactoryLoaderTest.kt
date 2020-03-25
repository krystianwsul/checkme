package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.FactoryProvider
import com.krystianwsul.checkme.firebase.ProjectFactory
import com.krystianwsul.checkme.firebase.RemoteUserFactory
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import org.junit.Test

class FactoryLoaderTest {

    private val local = object : FactoryProvider.Local {

        override val uuid = "uuid"

        override fun getShown(
                projectId: ProjectKey<*>,
                taskId: String,
                scheduleYear: Int,
                scheduleMonth: Int,
                scheduleDay: Int,
                scheduleCustomTimeId: CustomTimeId<*>?,
                scheduleHour: Int?,
                scheduleMinute: Int?
        ): Instance.Shown? = null

        override fun createShown(
                remoteTaskId: String,
                scheduleDateTime: DateTime,
                projectId: ProjectKey<*>
        ) = object : Instance.Shown {

            override var notified = false

            override var notificationShown = false
        }
    }

    private class TestDomain : FactoryProvider.Domain {

        override fun clearUserInfo() = Unit

        override fun updateFriendRecords(dataSnapshot: FactoryProvider.Database.Snapshot) = Unit

        override fun updateInstanceRecords(instanceEvent: ProjectFactory.InstanceEvent) = Unit

        override fun updatePrivateProjectRecord(dataSnapshot: FactoryProvider.Database.Snapshot) = Unit

        override fun updateSharedProjectRecords(sharedProjectEvent: ProjectFactory.SharedProjectEvent) = Unit

        override fun updateUserRecord(dataSnapshot: FactoryProvider.Database.Snapshot) = Unit
    }

    private class TestDatabase : FactoryProvider.Database() {

        val friendObservable = BehaviorRelay.create<Snapshot>()
        val privateProjectObservable = BehaviorRelay.create<Snapshot>()
        val rootInstanceObservable = BehaviorRelay.create<Snapshot>()
        val sharedProjectObservable = BehaviorRelay.create<Snapshot>()
        val userObservable = BehaviorRelay.create<Snapshot>()

        override fun getFriendObservable(userKey: UserKey) = friendObservable
        override fun getPrivateProjectObservable(key: ProjectKey.Private) = privateProjectObservable
        override fun getRootInstanceObservable(taskFirebaseKey: String) = rootInstanceObservable
        override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) = sharedProjectObservable
        override fun getUserObservable(key: UserKey) = userObservable

        override fun getNewId(path: String) = "id"

        override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) = Unit
    }

    private class TestPreferences : FactoryProvider.Preferences {

        override var tab = 0
    }

    private class TestFactoryProvider : FactoryProvider {

        override val nullableInstance: FactoryProvider.Domain? = null

        override val database = TestDatabase()

        override val preferences = TestPreferences()

        override fun newDomain(
                localFactory: FactoryProvider.Local,
                remoteUserFactory: RemoteUserFactory,
                projectFactory: ProjectFactory,
                deviceDbInfo: DeviceDbInfo,
                startTime: ExactTimeStamp,
                readTime: ExactTimeStamp,
                friendSnapshot: FactoryProvider.Database.Snapshot
        ) = TestDomain()
    }

    @Test
    fun testInitialize() {
        val deviceInfoObservable = BehaviorRelay.create<NullableWrapper<DeviceInfo>>()

        val factoryLoader = FactoryLoader(local, deviceInfoObservable, TestFactoryProvider())

        deviceInfoObservable.accept(NullableWrapper(DeviceInfo(
                UserInfo("email", "name"),
                "token"
        )))
    }
}