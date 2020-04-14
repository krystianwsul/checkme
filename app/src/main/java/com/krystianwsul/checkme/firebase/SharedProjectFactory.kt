package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.SharedProject
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.disposables.CompositeDisposable

class SharedProjectFactory(
        projectLoader: ProjectLoader<ProjectType.Shared>,
        initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Shared>,
        factoryProvider: FactoryProvider,
        domainDisposable: CompositeDisposable,
        private val deviceDbInfo: () -> DeviceDbInfo
) : ProjectFactory<ProjectType.Shared>(projectLoader, initialProjectEvent, factoryProvider, domainDisposable) {

    override fun newProject(projectRecord: ProjectRecord<ProjectType.Shared>) = SharedProject(
            projectRecord as SharedProjectRecord,
            rootInstanceManagers
    ) { newRootInstanceManager(it, listOf()) }.apply {
        fixNotificationShown(factoryProvider.shownFactory, ExactTimeStamp.now)
        updateDeviceDbInfo(deviceDbInfo()) // todo move making deviceInfo out of domainFactory, and feed changes into it (and here) instead
    }
}