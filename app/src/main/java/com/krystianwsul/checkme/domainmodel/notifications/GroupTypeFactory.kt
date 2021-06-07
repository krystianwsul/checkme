package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey

object GroupTypeFactory : GroupType.Factory {

    private fun GroupType.fix() = this as Bridge
    private fun GroupType.TimeChild.fix() = this as TimeChild
    private fun GroupType.InstanceDescriptor.fix() = this as InstanceDescriptor
    private fun GroupType.ProjectDescriptor.fix() = this as ProjectDescriptor

    fun getGroupTypeTree(
        instances: List<Instance>,
        groupingMode: GroupType.GroupingMode,
    ) = GroupType.getGroupTypeTree(this, instances.map(::InstanceDescriptor), groupingMode).map { it.fix() }

    override fun createTime(timeStamp: TimeStamp, groupTypes: List<GroupType.TimeChild>) =
        TimeBridge(timeStamp, groupTypes.map { it.fix() })

    override fun createTimeProject(
        timeStamp: TimeStamp,
        projectDescriptor: GroupType.ProjectDescriptor,
        instanceDescriptors: List<GroupType.InstanceDescriptor>,
    ): GroupType.TimeProject {
        val projectKey = projectDescriptor.fix().projectKey
        val instances = instanceDescriptors.map { it.fix().instance } // todo group strip project data

        return TimeProjectBridge(timeStamp, projectKey, instances)
    }

    override fun createProject(
        timeStamp: TimeStamp,
        projectDescriptor: GroupType.ProjectDescriptor,
        instanceDescriptors: List<GroupType.InstanceDescriptor>,
        nested: Boolean,
    ): GroupType.Project {
        val projectKey = projectDescriptor.fix().projectKey
        val instances = instanceDescriptors.map { it.fix().instance } // todo group strip project data

        return ProjectBridge(timeStamp, projectKey, instances)
    }

    override fun createSingle(instanceDescriptor: GroupType.InstanceDescriptor): GroupType.Single {
        val instance = instanceDescriptor.fix().instance

        return SingleBridge(instance)
    }

    class InstanceDescriptor(val instance: Instance) : GroupType.InstanceDescriptor {

        override val timeStamp get() = instance.instanceDateTime.timeStamp

        override val projectDescriptor = instance.getProject()
            .let { it as? SharedProject }
            ?.let(::ProjectDescriptor)
    }

    data class ProjectDescriptor(val projectKey: ProjectKey.Shared) : GroupType.ProjectDescriptor {

        constructor(sharedProject: SharedProject) : this(sharedProject.projectKey)
    }

    sealed interface Bridge

    sealed interface SingleParent : Bridge, GroupType.SingleParent

    sealed interface TimeChild : Bridge, GroupType.TimeChild {

        val instanceKeys: Set<InstanceKey>
    }

    class TimeBridge(val timeStamp: TimeStamp, private val timeChildren: List<TimeChild>) : GroupType.Time, SingleParent

    class TimeProjectBridge(
        val timeStamp: TimeStamp,
        val projectKey: ProjectKey.Shared,
        val instances: List<Instance>,
    ) : GroupType.TimeProject, SingleParent {

        val instanceKeys = instances.map { it.instanceKey }.toSet()
    }

    class ProjectBridge(
        val timeStamp: TimeStamp,
        val projectKey: ProjectKey.Shared,
        val instances: List<Instance>,
    ) : GroupType.Project, SingleParent, TimeChild {

        override val instanceKeys = instances.map { it.instanceKey }.toSet()
    }

    class SingleBridge(val instance: Instance) : GroupType.Single, TimeChild {

        override val instanceKeys = setOf(instance.instanceKey)
    }
}