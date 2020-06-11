package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.viewmodels.ShowProjectViewModel
import com.krystianwsul.common.firebase.models.SharedProject
import com.krystianwsul.common.utils.ProjectKey

@Synchronized
fun DomainFactory.getShowProjectData(projectId: ProjectKey.Shared?): ShowProjectViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowProjectData")

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