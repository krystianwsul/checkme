package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.ProjectListViewModel
import com.krystianwsul.common.domain.ProjectUndoData
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey

@Synchronized
fun DomainFactory.getProjectListData(): ProjectListViewModel.Data {
    MyCrashlytics.log("DomainFactory.getProjectListData")

    val remoteProjects = projectsFactory.sharedProjects

    val now = ExactTimeStamp.now

    val projectDatas = remoteProjects.values
        .filter { it.current(now) }
        .associate {
            val users = it.users.joinToString(", ") { it.name }

            it.projectKey to ProjectListViewModel.ProjectData(it.projectKey, it.name, users)
        }
        .toSortedMap()

    return ProjectListViewModel.Data(projectDatas)
}

@Synchronized
fun DomainFactory.setProjectEndTimeStamps(
    dataId: Int,
    source: SaveService.Source,
    projectIds: Set<ProjectKey<*>>,
    removeInstances: Boolean
): ProjectUndoData {
    MyCrashlytics.log("DomainFactory.setProjectEndTimeStamps")

    check(projectIds.isNotEmpty())

    val now = ExactTimeStamp.now

    val projectUndoData = ProjectUndoData()

    val remoteProjects = projectIds.map { projectsFactory.getProjectForce(it) }.toSet()

    remoteProjects.forEach {
        it.requireCurrent(now)
        it.setEndExactTimeStamp(now, projectUndoData, removeInstances)
    }

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(remoteProjects)

    return projectUndoData
}

@Synchronized
fun DomainFactory.clearProjectEndTimeStamps(
    dataId: Int,
    source: SaveService.Source,
    projectUndoData: ProjectUndoData
) {
    MyCrashlytics.log("DomainFactory.clearProjectEndTimeStamps")

    val now = ExactTimeStamp.now

    val remoteProjects = projectUndoData.projectIds
        .map { projectsFactory.getProjectForce(it) }
        .toSet()

    remoteProjects.forEach {
        it.requireNotCurrent(now)
        it.clearEndExactTimeStamp(now)
    }

    processTaskUndoData(projectUndoData.taskUndoData, now)

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(remoteProjects)
}