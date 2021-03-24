package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.DomainUpdater
import com.krystianwsul.checkme.viewmodels.ProjectListViewModel
import com.krystianwsul.common.domain.ProjectUndoData
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getProjectListData(): ProjectListViewModel.Data {
    MyCrashlytics.log("DomainFactory.getProjectListData")

    DomainThreadChecker.instance.requireDomainThread()

    val remoteProjects = projectsFactory.sharedProjects

    val now = ExactTimeStamp.Local.now

    val projectDatas = remoteProjects.values
            .filter { it.current(now) }
            .associate {
                val users = it.users.joinToString(", ") { it.name }

                it.projectKey to ProjectListViewModel.ProjectData(it.projectKey, it.name, users)
            }
            .toSortedMap()

    return ProjectListViewModel.Data(projectDatas)
}

@CheckResult
fun DomainUpdater.setProjectEndTimeStamps(
        notificationType: DomainListenerManager.NotificationType,
        projectIds: Set<ProjectKey<*>>,
        removeInstances: Boolean,
): Single<ProjectUndoData> = updateDomainSingle {
    MyCrashlytics.log("DomainFactory.setProjectEndTimeStamps")

    check(projectIds.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    val projectUndoData = ProjectUndoData()

    val remoteProjects = projectIds.map { projectsFactory.getProjectForce(it) }.toSet()

    remoteProjects.forEach {
        it.requireCurrent(now)
        it.setEndExactTimeStamp(now, projectUndoData, removeInstances)
    }

    notifier.updateNotifications(now)

    save(notificationType)

    DomainUpdater.Result(projectUndoData, DomainFactory.CloudParams(remoteProjects))
}

@CheckResult
fun DomainUpdater.clearProjectEndTimeStamps(
        notificationType: DomainListenerManager.NotificationType,
        projectUndoData: ProjectUndoData,
) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.clearProjectEndTimeStamps")

    check(projectUndoData.projectIds.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    val remoteProjects = projectUndoData.projectIds
            .map { projectsFactory.getProjectForce(it) }
            .toSet()

    remoteProjects.forEach {
        it.requireNotCurrent(now)
        it.clearEndExactTimeStamp(now)
    }

    processTaskUndoData(projectUndoData.taskUndoData, now)

    notifier.updateNotifications(now)

    save(notificationType)

    DomainUpdater.Params(DomainFactory.CloudParams(remoteProjects))
}