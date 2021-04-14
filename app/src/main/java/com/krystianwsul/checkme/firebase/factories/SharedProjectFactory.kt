package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.models.SharedProject
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.disposables.CompositeDisposable

class SharedProjectFactory(
        projectLoader: ProjectLoader<ProjectType.Shared, JsonWrapper>,
        initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Shared, JsonWrapper>,
        factoryProvider: FactoryProvider,
        domainDisposable: CompositeDisposable,
        deviceDbInfo: () -> DeviceDbInfo,
) : ProjectFactory<ProjectType.Shared, JsonWrapper>(
        projectLoader,
        initialProjectEvent,
        factoryProvider,
        domainDisposable,
        deviceDbInfo,
) {

    override fun newProject(projectRecord: ProjectRecord<ProjectType.Shared>) = SharedProject(
            projectRecord as SharedProjectRecord,
            object : JsonTime.UserCustomTimeProvider {

                override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                    TODO("todo customtime fetch")
                }
            }
    ).apply {
        fixNotificationShown(factoryProvider.shownFactory, ExactTimeStamp.Local.now)
        updateDeviceDbInfo(deviceDbInfo())
    }
}