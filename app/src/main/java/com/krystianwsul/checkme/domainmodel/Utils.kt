package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainQuery
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.interrupt.DomainInterruptedException
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.utils.ProjectKey

fun FirebaseUser.toUserInfo() = UserInfo(email!!, displayName!!, uid)

fun ImageState.toImageLoader() = ImageLoader(this)

fun <T : DomainData> getDomainResultInterrupting(action: () -> T): DomainQuery<T> {
    check(InterruptionChecker.instance == null)

    var interrupted = false

    InterruptionChecker.instance = InterruptionChecker { interrupted || Thread.interrupted() }

    return object : DomainQuery<T> {

        override fun getDomainResult(): DomainResult<T> {
            val domainResult = try {
                DomainResult.Completed(action())
            } catch (domainInterruptedException: DomainInterruptedException) {
                Log.e("asdf", "magic domain interrupted", domainInterruptedException)

                DomainResult.Interrupted()
            }

            checkNotNull(InterruptionChecker.instance)

            InterruptionChecker.instance = null

            return domainResult

        }

        override fun interrupt() {
            interrupted = true
        }
    }
}

fun <T> Sequence<T>.takeAndHasMore(n: Int): Pair<List<T>, Boolean> {
    val elementsPlusOne = take(n + 1).toList()

    val elements = elementsPlusOne.take(n)

    val hasMore = elements.size < elementsPlusOne.size

    return Pair(elements, hasMore)
}

fun Task.getProjectInfo(includeProjectDetails: Boolean = true): DetailsNode.ProjectInfo? {
    val sharedProjectKey = project.projectKey as? ProjectKey.Shared

    return if (isTopLevelTask() && sharedProjectKey != null) {
        DetailsNode.ProjectInfo(
            project.takeIf { includeProjectDetails }?.let { DetailsNode.ProjectDetails(it.name, sharedProjectKey) },
            DetailsNode.User.fromProjectUsers(getAssignedTo()),
        )
    } else {
        check(getAssignedTo().isEmpty())

        null
    }
}

fun Instance.getProjectInfo(includeProjectDetails: Boolean = true): DetailsNode.ProjectInfo? {
    val sharedProjectKey = getProject().projectKey as? ProjectKey.Shared

    return if (isRootInstance() && sharedProjectKey != null) {
        DetailsNode.ProjectInfo(
            task.project
                .takeIf { includeProjectDetails }
                ?.let { DetailsNode.ProjectDetails(it.name, sharedProjectKey) },
            DetailsNode.User.fromProjectUsers(getAssignedTo()),
        )
    } else {
        check(getAssignedTo().isEmpty())

        null
    }
}