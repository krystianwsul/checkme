package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.ProjectKey

interface GroupType {

    companion object {

        fun getGroupTypeTree(
            factory: Factory,
            instanceDescriptors: Collection<InstanceDescriptor>,
            groupingMode: GroupingMode,
        ): List<GroupType> {
            if (instanceDescriptors.isEmpty()) return emptyList()

            return when (groupingMode) {
                is GroupingMode.Time -> {
                    val timeGroups = instanceDescriptors.groupBy { it.timeStamp }

                    timeGroups.map { (timeStamp, instanceDescriptors) ->
                        check(instanceDescriptors.isNotEmpty())

                        // these are all instances at the same time
                        if (instanceDescriptors.size > 1) {
                            if (groupingMode.groupByProject) {
                                val projectDescriptor = instanceDescriptors.map { it.projectDescriptor }
                                    .distinct()
                                    .singleOrNull()

                                projectDescriptor?.let { factory.createTimeProject(timeStamp, it, instanceDescriptors) }
                                    ?: factory.createTime(
                                        timeStamp,
                                        groupByProject(factory, timeStamp, instanceDescriptors, true),
                                        groupingMode,
                                    )
                            } else {
                                factory.createTime(
                                    timeStamp,
                                    instanceDescriptors.map { factory.createTimeSingle(it) },
                                    groupingMode,
                                )
                            }
                        } else {
                            // if there's just one, there's our node
                            factory.createTopLevelSingle(instanceDescriptors.single())
                        }
                    }
                }
                GroupingMode.Projects -> { // don't group into time, but DO group into timeProject
                    val (projectInstances, noProjectInstances) =
                        instanceDescriptors.partition { it.projectDescriptor != null }

                    val noProjectGroupTypes = noProjectInstances.map { factory.createTopLevelSingle(it) }

                    val projectTimeGroups = projectInstances.groupBy { it.timeStamp to it.projectDescriptor!! }

                    val projectGroupTypes = projectTimeGroups.map { (groupPair, groupInstanceDescriptors) ->
                        check(groupInstanceDescriptors.isNotEmpty())

                        if (groupInstanceDescriptors.size > 1) {
                            val (timeStamp, projectDescriptor) = groupPair

                            factory.createTimeProject(timeStamp, projectDescriptor, groupInstanceDescriptors)
                        } else {
                            factory.createTopLevelSingle(groupInstanceDescriptors.single())
                        }
                    }

                    noProjectGroupTypes + projectGroupTypes
                }
                GroupingMode.Project -> {
                    val timeStamp = instanceDescriptors.map { it.timeStamp }
                        .distinct()
                        .single()

                    groupByProject(factory, timeStamp, instanceDescriptors, false)
                }
                is GroupingMode.Instance ->
                    groupByProject(factory, groupingMode.instanceTimeStamp, instanceDescriptors, false)
                GroupingMode.None -> instanceDescriptors.map { factory.createTopLevelSingle(it) }
            }
        }

        private fun groupByProject(
            factory: Factory,
            timeStamp: TimeStamp,
            instanceDescriptors: Collection<InstanceDescriptor>,
            nested: Boolean, // as in, nested in time vs. top-level
        ): List<TimeChild> {
            if (instanceDescriptors.isEmpty()) return emptyList()

            fun Factory.createSingle(instanceDescriptor: InstanceDescriptor) = if (nested) {
                createTimeSingle(instanceDescriptor)
            } else {
                createTopLevelSingle(instanceDescriptor)
            }

            val (projectInstances, noProjectInstances) = instanceDescriptors.partition { it.projectDescriptor != null }

            val projectGroups = projectInstances.groupBy { it.projectDescriptor!! }

            val groupTypesForShared = projectGroups.entries
                .map { (projectDescriptor, instanceDescriptors) ->
                    check(instanceDescriptors.isNotEmpty())

                    if (instanceDescriptors.size > 1) {
                        factory.createProject(timeStamp, projectDescriptor, instanceDescriptors)
                    } else {
                        factory.createSingle(instanceDescriptors.single())
                    }
                }

            val groupTypesForPrivate = noProjectInstances.map { factory.createSingle(it) }

            return listOf(groupTypesForShared, groupTypesForPrivate).flatten()
        }
    }

    interface SingleParent : GroupType
    interface TimeChild : GroupType

    interface Time : SingleParent

    /**
     * This is a stand-alone item, when there are instances of exclusively one project at a given time.  Example list:
     *
     * Time
     *      Project
     *      Single
     * TimeProject
     *      Single
     * Single
     */
    interface TimeProject : SingleParent

    /**
     * If nested, this is a child of Time on the main list:
     * Time
     *      Project
     *          Single
     *      Single
     *
     * Otherwise, it's a top-level node in a place where all instances have the same timeStamp, like ShowGroupActivity:
     * Project
     *      Single
     * Single
     */
    interface Project : TimeChild, SingleParent

    interface Single : TimeChild

    interface Factory {

        fun createTime(timeStamp: TimeStamp, groupTypes: List<TimeChild>, groupingMode: GroupingMode.Time): Time

        fun createTimeProject(
            timeStamp: TimeStamp,
            projectDescriptor: ProjectDescriptor,
            instanceDescriptors: List<InstanceDescriptor>,
        ): TimeProject

        fun createProject(
            timeStamp: TimeStamp,
            projectDescriptor: ProjectDescriptor,
            instanceDescriptors: List<InstanceDescriptor>,
        ): Project

        fun createTopLevelSingle(instanceDescriptor: InstanceDescriptor): Single
        fun createTimeSingle(instanceDescriptor: InstanceDescriptor): Single
    }

    interface InstanceDescriptor {

        val timeStamp: TimeStamp

        val projectDescriptor: ProjectDescriptor?
    }

    interface ProjectDescriptor

    sealed interface GroupingMode {

        object None : GroupingMode

        // group by project, assume single time
        object Project : GroupingMode

        // group by time and project
        class Time(private val projectKey: ProjectKey.Shared? = null) : GroupingMode {

            val groupByProject = projectKey == null

            fun newShowGroupActivityParameters(timeStamp: TimeStamp) =
                projectKey?.let { ShowGroupActivity.Parameters.Project(timeStamp, it) }
                    ?: ShowGroupActivity.Parameters.Time(timeStamp)
        }

        // group by time, assume single project
        object Projects : GroupingMode

        // group by project (for those that don't match parent instance project), assume single time
        class Instance(val instanceTimeStamp: TimeStamp) : GroupingMode
    }
}