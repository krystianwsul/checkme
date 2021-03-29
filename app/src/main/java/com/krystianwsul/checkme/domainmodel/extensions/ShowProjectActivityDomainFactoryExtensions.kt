package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
import com.krystianwsul.checkme.viewmodels.ShowProjectViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.SharedProject
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getShowProjectData(projectId: ProjectKey.Shared?): ShowProjectViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowProjectData")

    DomainThreadChecker.instance.requireDomainThread()

    val friendDatas = friendsFactory.friends
            .map { ShowProjectViewModel.UserListData(it.name, it.email, it.userKey, it.photoUrl) }
            .associateBy { it.id }

    val name: String?
    val userListDatas: Set<ShowProjectViewModel.UserListData>
    if (projectId != null) {
        val remoteProject = projectsFactory.getProjectForce(projectId) as SharedProject

        name = remoteProject.name

        userListDatas = remoteProject.users
                .filterNot { it.id == deviceDbInfo.key }
                .map { ShowProjectViewModel.UserListData(it.name, it.email, it.id, it.photoUrl) }
                .toSet()
    } else {
        name = null
        userListDatas = setOf()
    }

    return ShowProjectViewModel.Data(name, userListDatas, friendDatas)
}

@CheckResult
fun DomainUpdater.createProject(
        notificationType: DomainListenerManager.NotificationType,
        name: String,
        friends: Set<UserKey>,
): Single<ProjectKey.Shared> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.createProject")

    check(name.isNotEmpty())

    val recordOf = friends.toMutableSet()

    val key = deviceDbInfo.key
    check(!recordOf.contains(key))
    recordOf.add(key)

    val remoteProject = projectsFactory.createProject(
            name,
            now,
            recordOf,
            myUserFactory.user,
            deviceDbInfo.userInfo,
            friendsFactory,
    )

    myUserFactory.user.addProject(remoteProject.projectKey)
    friendsFactory.updateProjects(remoteProject.projectKey, friends, setOf())

    DomainUpdater.Result(
            remoteProject.projectKey,
            false,
            notificationType,
            DomainFactory.CloudParams(remoteProject),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.updateProject(
        notificationType: DomainListenerManager.NotificationType,
        projectId: ProjectKey.Shared,
        name: String,
        addedFriends: Set<UserKey>,
        removedFriends: Set<UserKey>,
): Completable = CompletableDomainUpdate.create {
    MyCrashlytics.log("DomainFactory.updateProject")

    check(name.isNotEmpty())

    val remoteProject = projectsFactory.getProjectForce(projectId) as SharedProject

    remoteProject.name = name

    remoteProject.updateUsers(
            addedFriends.map { friendsFactory.getFriend(it) }.toSet(),
            removedFriends,
    )

    friendsFactory.updateProjects(projectId, addedFriends, removedFriends)

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(remoteProject, removedFriends))
}.perform(this)