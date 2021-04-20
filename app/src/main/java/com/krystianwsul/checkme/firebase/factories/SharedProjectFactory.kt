package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.models.SharedProject
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.JsonTime
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

    override fun newProject(
            projectRecord: ProjectRecord<ProjectType.Shared>,
            userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    ) = SharedProject(projectRecord as SharedProjectRecord, userCustomTimeProvider).apply {
        fixNotificationShown(factoryProvider.shownFactory, ExactTimeStamp.Local.now)
        updateDeviceDbInfo(deviceDbInfo())
    }
}