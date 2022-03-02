package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.time.ExactTimeStamp

object HasInstancesStore {

    fun init() = Unit

    private fun hasInstances(task: RootTask, now: ExactTimeStamp.Local) =
        task.getInstances(null, null, now).any()

    fun update(domainFactory: DomainFactory, now: ExactTimeStamp.Local) {
        domainFactory.myUserFactory
            .user
            .let { it.projectIds + it.userKey.toPrivateProjectKey() }
            .map { domainFactory.projectsFactory.getProjectForce(it) }
            .flatMap { it.projectRecord.rootTaskParentDelegate.rootTaskKeys }
            .toSet()
            .all { domainFactory.rootTasksFactory.getRootTaskIfPresent(it)?.dependenciesLoaded == true }
            .takeIf { !it }
            ?.let { return }

        domainFactory.rootTasksFactory
            .rootTasks
            .all { it.value.dependenciesLoaded }
            .takeIf { !it }
            ?.let { return }

        val hasInstancesMap = domainFactory.rootTasksFactory
            .rootTasks
            .mapValues { hasInstances(it.value, now) }


    }
}