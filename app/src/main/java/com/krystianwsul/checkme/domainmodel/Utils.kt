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
import com.krystianwsul.common.firebase.models.project.SharedOwnedProject
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
    val sharedProject = project as? SharedOwnedProject

    return if (isTopLevelTask() && sharedProject != null) {
        DetailsNode.ProjectInfo(
            sharedProject.takeIf { includeProjectDetails }?.let {
                DetailsNode.ProjectDetails(it.name, sharedProject.projectKey)
            },
            DetailsNode.User.fromProjectUsers(getAssignedTo()),
        )
    } else {
        check(getAssignedTo().isEmpty())

        null
    }
}

sealed class ProjectInfoMode {

    open val showForChildren = false

    abstract fun showProject(sharedOwnedProject: SharedOwnedProject): Boolean

    object Hide : ProjectInfoMode() {

        override fun showProject(sharedOwnedProject: SharedOwnedProject) = false
    }

    object Show : ProjectInfoMode() { // group hack

        override fun showProject(sharedOwnedProject: SharedOwnedProject) = true
    }

    class ShowInsideInstance(private val parentInstanceKey: ProjectKey.Shared? = null) : ProjectInfoMode() { // group hack

        companion object {

            fun fromProjectKey(projectKey: ProjectKey<*>) =
                ShowInsideInstance(projectKey as? ProjectKey.Shared) // group hack
        }

        override val showForChildren = true

        override fun showProject(sharedOwnedProject: SharedOwnedProject) = sharedOwnedProject.projectKey != parentInstanceKey
    }
}

fun Instance.getProjectInfo(projectInfoMode: ProjectInfoMode = ProjectInfoMode.Show): DetailsNode.ProjectInfo? {
    val sharedProject = getProject() as? SharedOwnedProject

    return if (sharedProject != null && (isRootInstance() || projectInfoMode.showForChildren)) {
        DetailsNode.ProjectInfo(
            sharedProject.takeIf { projectInfoMode.showProject(it) }?.let {
                DetailsNode.ProjectDetails(it.name, sharedProject.projectKey)
            },
            DetailsNode.User.fromProjectUsers(getAssignedTo()),
        )
    } else {
        check(getAssignedTo().isEmpty())

        null
    }
}