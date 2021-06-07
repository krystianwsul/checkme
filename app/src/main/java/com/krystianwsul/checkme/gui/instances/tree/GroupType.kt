package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.common.time.TimeStamp

interface GroupType {

    companion object {

        fun getGroupTypeTree(
            factory: Factory,
            instanceDatas: List<InstanceDescriptor>,
            groupingMode: NodeCollection.GroupingMode,
        ): List<GroupType> {
            if (instanceDatas.isEmpty()) return emptyList()

            return when (groupingMode) {
                NodeCollection.GroupingMode.TIME -> {
                    val timeGroups = instanceDatas.groupBy { it.timeStamp }

                    timeGroups.map { (timeStamp, instanceDescriptors) ->
                        check(instanceDescriptors.isNotEmpty())

                        // these are all instances at the same time
                        if (instanceDescriptors.size > 1) {
                            val projectDescriptor = instanceDescriptors.map { it.projectDescriptor }
                                .distinct()
                                .singleOrNull()

                            projectDescriptor?.let {
                                factory.createTimeProject(timeStamp, projectDescriptor, instanceDescriptors)
                            } ?: factory.createTime(
                                timeStamp,
                                groupByProject(factory, timeStamp, instanceDescriptors, true)
                            )
                        } else {
                            // if there's just one, there's our node
                            factory.createSingle(instanceDescriptors.single())
                        }
                    }
                }
                NodeCollection.GroupingMode.PROJECT -> {
                    val timeStamp = instanceDatas.map { it.timeStamp }
                        .distinct()
                        .single()

                    groupByProject(factory, timeStamp, instanceDatas, false)
                }
                NodeCollection.GroupingMode.NONE -> instanceDatas.map(factory::createSingle)
            }
        }

        private fun groupByProject(
            factory: Factory,
            timeStamp: TimeStamp,
            instanceDescriptor: List<InstanceDescriptor>,
            nested: Boolean,
        ): List<TimeChild> {
            if (instanceDescriptor.isEmpty()) return emptyList()

            val projectGroups = instanceDescriptor.groupBy { it.projectDescriptor }

            val groupTypesForShared = projectGroups.entries
                .filter { it.key != null }
                .map { (projectDescriptor, instanceDescriptors) ->
                    check(instanceDescriptors.isNotEmpty())

                    if (instanceDescriptors.size > 1) {
                        factory.createProject(timeStamp, projectDescriptor!!, instanceDescriptors, nested)
                    } else {
                        factory.createSingle(instanceDescriptors.single())
                    }
                }

            val groupTypesForPrivate = projectGroups[null]?.map(factory::createSingle).orEmpty()

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

        fun createTime(timeStamp: TimeStamp, groupTypes: List<TimeChild>): Time

        fun createTimeProject(
            timeStamp: TimeStamp,
            projectDescriptor: ProjectDescriptor,
            instanceDescriptors: List<InstanceDescriptor>,
        ): TimeProject

        fun createProject(
            timeStamp: TimeStamp,
            projectDescriptor: ProjectDescriptor,
            instanceDescriptors: List<InstanceDescriptor>,
            nested: Boolean,
        ): Project

        fun createSingle(instanceDescriptor: InstanceDescriptor): Single
    }

    interface InstanceDescriptor {

        val timeStamp: TimeStamp

        val projectDescriptor: ProjectDescriptor?
    }

    interface ProjectDescriptor
}