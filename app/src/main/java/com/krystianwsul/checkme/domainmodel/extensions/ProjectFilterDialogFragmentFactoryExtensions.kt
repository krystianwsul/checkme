package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.main.ProjectFilterViewModel

fun DomainFactory.getProjectFilterData(): ProjectFilterViewModel.Data {
    MyCrashlytics.log("DomainFactory.getProjectFilterData")

    return projectsFactory.sharedProjects
        .values
        .filter { it.notDeleted }
        .map { ProjectFilterViewModel.Project(it.projectKey, it.name) }
        .sortedBy { it.projectKey }
        .let(ProjectFilterViewModel::Data)
}