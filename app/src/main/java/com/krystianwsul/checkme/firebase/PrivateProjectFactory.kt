package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.firebase.models.PrivateProject
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.disposables.CompositeDisposable

class PrivateProjectFactory(
        projectLoader: ProjectLoader<ProjectType.Private>,
        initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private>,
        factoryProvider: FactoryProvider,
        domainDisposable: CompositeDisposable
) : ProjectFactory<ProjectType.Private>(projectLoader, initialProjectEvent, factoryProvider, domainDisposable) {

    override fun newProject(projectRecord: ProjectRecord<ProjectType.Private>) = PrivateProject(
            projectRecord as PrivateProjectRecord,
            rootInstanceManagers
    ) { newRootInstanceManager(it, listOf()) }
}