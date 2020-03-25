package com.krystianwsul.checkme.domainmodel

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.ProjectFactory
import com.krystianwsul.checkme.firebase.RemoteUserFactory
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp

interface FactoryProvider {

    val nullableInstance: Domain?

    fun newDomain(
            localFactory: Local,
            remoteUserFactory: RemoteUserFactory,
            projectFactory: ProjectFactory,
            deviceDbInfo: DeviceDbInfo,
            startTime: ExactTimeStamp,
            readTime: ExactTimeStamp,
            friendSnapshot: DataSnapshot
    ): Domain

    interface Domain {

        fun updatePrivateProjectRecord(dataSnapshot: DataSnapshot)

        fun updateSharedProjectRecords(sharedProjectEvent: ProjectFactory.SharedProjectEvent)

        fun updateFriendRecords(dataSnapshot: DataSnapshot)

        fun updateUserRecord(dataSnapshot: DataSnapshot)

        fun updateInstanceRecords(instanceEvent: ProjectFactory.InstanceEvent)

        fun clearUserInfo()
    }

    interface Local : Instance.ShownFactory {

        val uuid: String
    }

    object Impl : FactoryProvider {

        override val nullableInstance get() = DomainFactory.nullableInstance

        override fun newDomain(
                localFactory: Local,
                remoteUserFactory: RemoteUserFactory,
                projectFactory: ProjectFactory,
                deviceDbInfo: DeviceDbInfo,
                startTime: ExactTimeStamp,
                readTime: ExactTimeStamp,
                friendSnapshot: DataSnapshot
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