package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.interrupt.DomainInterruptedException
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.UserKey

fun FirebaseUser.toUserInfo() = UserInfo(email!!, displayName!!, uid)

fun ImageState.toImageLoader() = ImageLoader(this)

fun <T : DomainData> getDomainResultInterrupting(action: () -> T): DomainResult<T> {
    return try {
        DomainResult.Completed(action())
    } catch (domainInterruptedException: DomainInterruptedException) {
        Log.e("asdf", "domain interrupted", domainInterruptedException)

        DomainResult.Interrupted()
    }
}

fun <T> Sequence<T>.takeAndHasMore(n: Int): Pair<List<T>, Boolean> {
    val elementsPlusOne = take(n + 1).toList()

    val elements = elementsPlusOne.take(n)

    val hasMore = elements.size < elementsPlusOne.size

    return Pair(elements, hasMore)
}

fun Task<*>.getProjectInfo(now: ExactTimeStamp.Local): DetailsNode.ProjectInfo? {
    return if (isRootTask(getHierarchyExactTimeStamp(now)) && project is SharedProject) {
        DetailsNode.ProjectInfo(project.name, DetailsNode.User.fromProjectUsers(getAssignedTo(now)))
    } else {
        check(getAssignedTo(now).isEmpty())

        null
    }
}

fun Instance<*>.getProjectInfo(now: ExactTimeStamp.Local): DetailsNode.ProjectInfo? {
    return if (isRootInstance() && task.project is SharedProject) {
        DetailsNode.ProjectInfo(task.project.name, DetailsNode.User.fromProjectUsers(getAssignedTo(now)))
    } else {
        check(getAssignedTo(now).isEmpty())

        null
    }
}

fun Instance<*>.toGroupListData(
        now: ExactTimeStamp.Local,
        ownerKey: UserKey,
        children: MutableMap<InstanceKey, GroupListDataWrapper.InstanceData>,
        localFactory: LocalFactory,
        deviceDbInfo: DeviceDbInfo,
        user: MyUser,
): GroupListDataWrapper.InstanceData {
    val isRootInstance = isRootInstance()

    return GroupListDataWrapper.InstanceData(
            done,
            instanceKey,
            if (isRootInstance) instanceDateTime.getDisplayText() else null,
            name,
            instanceDateTime.timeStamp,
            instanceDateTime,
            task.current(now),
            canAddSubtask(now),
            isRootInstance(),
            getCreateTaskTimePair(ownerKey),
            task.note,
            children,
            task.ordinal,
            getNotificationShown(localFactory),
            task.getImage(deviceDbInfo),
            isGroupChild(),
            isAssignedToMe(now, user),
            getProjectInfo(now),
    )
}