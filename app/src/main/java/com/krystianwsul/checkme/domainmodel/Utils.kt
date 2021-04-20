package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.interrupt.DomainInterruptedException
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.ExactTimeStamp

fun FirebaseUser.toUserInfo() = UserInfo(email!!, displayName!!, uid)

fun ImageState.toImageLoader() = ImageLoader(this)

fun <T : DomainData> getDomainResultInterrupting(action: () -> T): DomainResult<T> {
    check(InterruptionChecker.instance == null)

    InterruptionChecker.instance = InterruptionChecker { Thread.interrupted() }

    val domainResult = try {
        DomainResult.Completed(action())
    } catch (domainInterruptedException: DomainInterruptedException) {
        Log.e("asdf", "domain interrupted", domainInterruptedException)

        DomainResult.Interrupted()
    }

    checkNotNull(InterruptionChecker.instance)

    InterruptionChecker.instance = null

    return domainResult
}

fun <T> Sequence<T>.takeAndHasMore(n: Int): Pair<List<T>, Boolean> {
    val elementsPlusOne = take(n + 1).toList()

    val elements = elementsPlusOne.take(n)

    val hasMore = elements.size < elementsPlusOne.size

    return Pair(elements, hasMore)
}

fun Task.getProjectInfo(now: ExactTimeStamp.Local): DetailsNode.ProjectInfo? {
    return if (isTopLevelTask(getHierarchyExactTimeStamp(now)) && project is SharedProject) {
        DetailsNode.ProjectInfo(project.name, DetailsNode.User.fromProjectUsers(getAssignedTo(now)))
    } else {
        check(getAssignedTo(now).isEmpty())

        null
    }
}

fun Instance.getProjectInfo(now: ExactTimeStamp.Local): DetailsNode.ProjectInfo? {
    return if (isRootInstance() && task.project is SharedProject) {
        DetailsNode.ProjectInfo(task.project.name, DetailsNode.User.fromProjectUsers(getAssignedTo(now)))
    } else {
        check(getAssignedTo(now).isEmpty())

        null
    }
}