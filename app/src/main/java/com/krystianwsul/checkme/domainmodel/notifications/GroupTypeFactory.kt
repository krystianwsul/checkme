package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey

object GroupTypeFactory : GroupType.Factory {

    private fun GroupType.fix() = this as Bridge
    private fun GroupType.TimeChild.fix() = this as TimeChild
    private fun GroupType.InstanceDescriptor.fix() = this as InstanceDescriptor
    private fun GroupType.ProjectDescriptor.fix() = this as ProjectDescriptor

    fun getGroupTypeTree(
        instanceDescriptors: List<InstanceDescriptor>,
        groupingMode: GroupType.GroupingMode,
    ) = GroupType.getGroupTypeTree(this, instanceDescriptors, groupingMode).map { it.fix() }

    override fun createTime(
        timeStamp: TimeStamp,
        groupTypes: List<GroupType.TimeChild>,
        groupingMode: GroupType.GroupingMode.Time
    ) = TimeBridge(timeStamp, groupTypes.map { it.fix() })

    override fun createTimeProject(
        timeStamp: TimeStamp,
        projectDescriptor: GroupType.ProjectDescriptor,
        instanceDescriptors: List<GroupType.InstanceDescriptor>,
    ) = TimeProjectBridge(timeStamp, projectDescriptor.fix().project, instanceDescriptors.map { it.fix() })

    override fun createProject(
        timeStamp: TimeStamp,
        projectDescriptor: GroupType.ProjectDescriptor,
        instanceDescriptors: List<GroupType.InstanceDescriptor>,
    ) = ProjectBridge(timeStamp, projectDescriptor.fix().project, instanceDescriptors.map { it.fix() })

    override fun createSingle(instanceDescriptor: GroupType.InstanceDescriptor) = SingleBridge(instanceDescriptor.fix())

    class InstanceDescriptor(val instance: Instance, val silent: Boolean) : GroupType.InstanceDescriptor {

        override val timeStamp get() = instance.instanceDateTime.timeStamp

        override val projectDescriptor = instance.getProject()
            .let { it as? SharedProject }
            ?.let(::ProjectDescriptor)

        override val groupIntoProject get() = instance.groupIntoProject
    }

    data class ProjectDescriptor(val project: SharedProject) : GroupType.ProjectDescriptor

    sealed interface Bridge {

        fun getNotifications(): List<Notification>
    }

    sealed interface SingleParent : Bridge, GroupType.SingleParent

    sealed interface TimeChild : Bridge, GroupType.TimeChild {

        val instanceKeys: Set<InstanceKey>
    }

    class TimeBridge(val timeStamp: TimeStamp, private val timeChildren: List<TimeChild>) : GroupType.Time, SingleParent {

        override fun getNotifications() = timeChildren.flatMap { it.getNotifications() }
    }

    class TimeProjectBridge(
        val timeStamp: TimeStamp,
        val project: SharedProject,
        val instanceDescriptors: List<InstanceDescriptor>,
    ) : GroupType.TimeProject, SingleParent {

        val instanceKeys = instanceDescriptors.map { it.instance.instanceKey }.toSet()

        override fun getNotifications() = listOf(Notification.Project(project, instanceDescriptors, timeStamp))
    }

    class ProjectBridge(
        val timeStamp: TimeStamp,
        val project: SharedProject,
        val instanceDescriptors: List<InstanceDescriptor>,
    ) : GroupType.Project, SingleParent, TimeChild {

        override val instanceKeys = instanceDescriptors.map { it.instance.instanceKey }.toSet()

        override fun getNotifications() = listOf(Notification.Project(project, instanceDescriptors, timeStamp))
    }

    class SingleBridge(val instanceDescriptor: InstanceDescriptor) : GroupType.Single, TimeChild {

        override val instanceKeys = setOf(instanceDescriptor.instance.instanceKey)

        override fun getNotifications() =
            listOf(Notification.Instance(instanceDescriptor.instance, instanceDescriptor.silent))
    }

    sealed class Notification {

        class Instance(val instance: com.krystianwsul.common.firebase.models.Instance, val silent: Boolean) : Notification()

        class Project(
            val project: SharedProject,
            val instances: List<com.krystianwsul.common.firebase.models.Instance>,
            val timeStamp: TimeStamp,
            val silent: Boolean,
        ) : Notification() {

            constructor(project: SharedProject, instanceDescriptors: List<InstanceDescriptor>, timeStamp: TimeStamp) : this(
                project,
                instanceDescriptors.map { it.instance },
                timeStamp,
                instanceDescriptors.all { it.silent },
            )
        }
    }
}