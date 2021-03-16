package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.ShowProjectViewModel
import com.krystianwsul.common.firebase.SchedulerType
import com.krystianwsul.common.firebase.SchedulerTypeHolder
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.models.SharedProject
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey

fun DomainFactory.getShowProjectData(projectId: ProjectKey.Shared?): ShowProjectViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowProjectData")

    SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

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

fun DomainFactory.createProject(
        dataId: Int,
        source: SaveService.Source,
        name: String,
        friends: Set<UserKey>
) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.createProject")

    check(name.isNotEmpty())

    val now = ExactTimeStamp.Local.now

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
        friendsFactory
    )

    myUserFactory.user.addProject(remoteProject.projectKey)
    friendsFactory.updateProjects(remoteProject.projectKey, friends, setOf())

    save(dataId, source)

    notifyCloud(remoteProject)
}

fun DomainFactory.updateProject(
        dataId: Int,
        source: SaveService.Source,
        projectId: ProjectKey.Shared,
        name: String,
        addedFriends: Set<UserKey>,
        removedFriends: Set<UserKey>
) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.updateProject")

    check(name.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    val remoteProject = projectsFactory.getProjectForce(projectId) as SharedProject

    remoteProject.name = name
    remoteProject.updateUsers(
            addedFriends.map { friendsFactory.getFriend(it) }.toSet(),
            removedFriends
    )

    friendsFactory.updateProjects(projectId, addedFriends, removedFriends)

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(remoteProject, removedFriends)
}

private fun DomainFactory.notifyCloud(
    project: Project<*>,
    userKeys: Collection<UserKey>
) = notifyCloudPrivateFixed(mutableSetOf(project), userKeys.toMutableList())