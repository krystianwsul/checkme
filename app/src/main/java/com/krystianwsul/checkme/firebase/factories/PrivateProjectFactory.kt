package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.models.PrivateProject
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PrivateProjectFactory(
        projectLoader: ProjectLoader<ProjectType.Private, PrivateProjectJson>,
        initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private, PrivateProjectJson>,
        factoryProvider: FactoryProvider,
        domainDisposable: CompositeDisposable,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
        deviceDbInfo: () -> DeviceDbInfo,
) : ProjectFactory<ProjectType.Private, PrivateProjectJson>(
        projectLoader,
        initialProjectEvent,
        factoryProvider,
        domainDisposable,
        userCustomTimeProvider,
        deviceDbInfo,
) {

    override fun newProject(projectRecord: ProjectRecord<ProjectType.Private>) =
            PrivateProject(projectRecord as PrivateProjectRecord, userCustomTimeProvider)
}