package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PrivateProjectFactory(
        projectLoader: ProjectLoader<ProjectType.Private, PrivateProjectJson>,
        initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private, PrivateProjectJson>,
        factoryProvider: FactoryProvider,
        domainDisposable: CompositeDisposable,
        rootTaskProvider: Project.RootTaskProvider,
        deviceDbInfo: () -> DeviceDbInfo,
) : ProjectFactory<ProjectType.Private, PrivateProjectJson>(
        projectLoader,
        initialProjectEvent,
        factoryProvider,
        domainDisposable,
        rootTaskProvider,
        deviceDbInfo,
) {

    override fun newProject(
            projectRecord: ProjectRecord<ProjectType.Private>,
            userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
            rootTaskProvider: Project.RootTaskProvider,
    ) = PrivateProject(projectRecord as PrivateProjectRecord, userCustomTimeProvider, rootTaskProvider)
}