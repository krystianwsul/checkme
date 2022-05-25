package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
import com.krystianwsul.checkme.viewmodels.ProjectListViewModel
import com.krystianwsul.common.domain.ProjectUndoData
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getProjectListData(): ProjectListViewModel.Data {
    MyCrashlytics.log("DomainFactory.getProjectListData")

    DomainThreadChecker.instance.requireDomainThread()

    val remoteProjects = projectsFactory.sharedProjects

    val projectDatas = remoteProjects.values
        .filter { it.notDeleted }
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
    projectKeys: Set<ProjectKey.Shared>,
    removeInstances: Boolean,
): Single<ProjectUndoData> = SingleDomainUpdate.create("setProjectEndTimeStamps") { now ->
    check(projectKeys.isNotEmpty())

    val projectUndoData = ProjectUndoData()

    val remoteProjects = projectKeys.map { projectsFactory.getSharedProjectForce(it) }.toSet()

    remoteProjects.forEach {
        it.requireNotDeleted()
        it.setEndExactTimeStamp(now, projectUndoData, removeInstances)
    }

    DomainUpdater.Result(projectUndoData, true, notificationType, DomainFactory.CloudParams(remoteProjects))
}.perform(this)

@CheckResult
fun DomainUpdater.clearProjectEndTimeStamps(
    notificationType: DomainListenerManager.NotificationType,
    projectUndoData: ProjectUndoData,
): Completable = CompletableDomainUpdate.create("clearProjectEndTimeStamps") {
    check(projectUndoData.projectKeys.isNotEmpty())

    val remoteProjects = projectUndoData.projectKeys
        .map { projectsFactory.getSharedProjectForce(it) }
        .toSet()

    remoteProjects.forEach {
        it.requireDeleted()
        it.clearEndExactTimeStamp()
    }

    processTaskUndoData(projectUndoData.taskUndoData)

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(remoteProjects))
}.perform(this)