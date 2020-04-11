package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.firebase.models.SharedProject
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.disposables.CompositeDisposable

class SharedProjectFactory(
        projectLoader: ProjectLoader<ProjectType.Shared>,
        initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Shared>,
        factoryProvider: FactoryProvider,
        domainDisposable: CompositeDisposable
) : ProjectFactory<ProjectType.Shared>(projectLoader, initialProjectEvent, factoryProvider, domainDisposable) {

    override fun newProject(projectRecord: ProjectRecord<ProjectType.Shared>) = SharedProject(
            projectRecord as SharedProjectRecord,
            rootInstanceManagers
    ) { newRootInstanceManager(it, listOf()) }
}