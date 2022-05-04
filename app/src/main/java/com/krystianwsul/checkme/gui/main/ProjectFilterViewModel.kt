package com.krystianwsul.checkme.gui.main

import com.krystianwsul.checkme.domainmodel.extensions.getProjectFilterData
import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainListener
import com.krystianwsul.checkme.viewmodels.DomainViewModel
import com.krystianwsul.common.utils.ProjectKey

class ProjectFilterViewModel : DomainViewModel<ProjectFilterViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getProjectFilterData() }
    }

    fun start() = internalStart()

    data class Data(val projects: List<Project>) : DomainData()

    data class Project(val projectKey: ProjectKey.Shared, val name: String)
}